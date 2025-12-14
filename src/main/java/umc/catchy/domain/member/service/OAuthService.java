package umc.catchy.domain.member.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.ReadOnlyJWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import net.minidev.json.JSONObject;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.stereotype.Service;
import umc.catchy.domain.member.domain.SocialType;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.config.auth.AuthConfig;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.infra.feign.AppleFeignClient;
import umc.catchy.infra.feign.KakaoAuthClient;
import umc.catchy.infra.feign.KakaoFeignClient;
import umc.catchy.infra.feign.dto.AppleTokenResponse;
import umc.catchy.infra.feign.dto.KakaoInfoResponse;
import umc.catchy.infra.feign.dto.KakaoTokenResponse;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.security.PrivateKey;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OAuthService {

    private static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
    private static final String APPLE_ALGORITHM = "ES256";
    private static final int CLIENT_SECRET_EXPIRATION_DAYS = 30;

    private final AuthConfig authConfig;
    private final KakaoFeignClient kakaoFeignClient;
    private final KakaoAuthClient kakaoAuthClient;
    private final AppleFeignClient appleFeignClient;

    public SocialUserInfo getUserInfo(String token, SocialType socialType) {
        return switch (socialType) {
            case KAKAO -> getKakaoInfo(token);
            case APPLE -> getAppleInfo(token);
        };
    }

    public String getKakaoAccessToken(String code) {
        try {
            KakaoTokenResponse response = kakaoAuthClient.getAccessToken(
                    GRANT_TYPE_AUTHORIZATION_CODE,
                    authConfig.KAKAO_CLIENT_ID,
                    authConfig.KAKAO_REDIRECT_URL,
                    code,
                    authConfig.KAKAO_CLIENT_SECRET
            );
            return response.accessToken();
        } catch (Exception e) {
            throw new GeneralException(ErrorStatus.SOCIAL_MEMBER_NOT_FOUND);
        }
    }

    public void appleWithdraw(String authorizationCode) throws IOException {
        String clientSecret = createClientSecret();
        AppleTokenResponse tokenResponse = appleFeignClient.getToken(
                authConfig.APPLE_CLIENT_ID,
                clientSecret,
                authorizationCode,
                GRANT_TYPE_AUTHORIZATION_CODE,
                authConfig.APPLE_REDIRECT_URL
        );

        if (tokenResponse != null && tokenResponse.accessToken() != null) {
            appleFeignClient.revokeToken(
                    authConfig.APPLE_CLIENT_ID,
                    clientSecret,
                    tokenResponse.accessToken()
            );
        }
    }

    private SocialUserInfo getKakaoInfo(String accessToken) {
        try {
            KakaoInfoResponse response = kakaoFeignClient.getUserInfo("Bearer " + accessToken);
            return new SocialUserInfo(
                    String.valueOf(response.id()),
                    response.kakaoAccount().email()
            );
        } catch (Exception e) {
            throw new GeneralException(ErrorStatus.SOCIAL_MEMBER_NOT_FOUND);
        }
    }

    private SocialUserInfo getAppleInfo(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            ReadOnlyJWTClaimsSet getPayload = signedJWT.getJWTClaimsSet();

            ObjectMapper objectMapper = new ObjectMapper();
            JSONObject payload = objectMapper.readValue(getPayload.toJSONObject().toJSONString(), JSONObject.class);

            String providerId = String.valueOf(payload.get("sub"));
            String email = String.valueOf(payload.get("email"));

            return new SocialUserInfo(providerId, email);
        } catch (ParseException | JsonProcessingException e) {
            throw new GeneralException(ErrorStatus.SOCIAL_MEMBER_NOT_FOUND);
        }
    }

    private String createClientSecret() throws IOException {
        Date expirationDate = Date.from(
                LocalDateTime.now()
                        .plusDays(CLIENT_SECRET_EXPIRATION_DAYS)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
        );

        Map<String, Object> jwtHeader = new HashMap<>();
        jwtHeader.put("kid", authConfig.APPLE_KEY_ID);
        jwtHeader.put("alg", APPLE_ALGORITHM);

        return Jwts.builder()
                .setHeaderParams(jwtHeader)
                .setIssuer(authConfig.APPLE_TEAM_ID)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(expirationDate)
                .setAudience(authConfig.APPLE_REQUEST_URL)
                .setSubject(authConfig.APPLE_CLIENT_ID)
                .signWith(SignatureAlgorithm.ES256, getPrivateKey())
                .compact();
    }

    private PrivateKey getPrivateKey() throws IOException {
        String privateKey = authConfig.APPLE_PRIVATE_KEY.replace("\\\\", "\\").replace("\\n", "\n");
        Reader pemReader = new StringReader(privateKey);
        PEMParser pemParser = new PEMParser(pemReader);
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        try {
            PrivateKeyInfo object = (PrivateKeyInfo) pemParser.readObject();
            return converter.getPrivateKey(object);
        } catch (IOException e) {
            throw new GeneralException(ErrorStatus.AUTHORIZATION_CODE_UNAUTHORIZED);
        }
    }

    public record SocialUserInfo(String providerId, String email) {}
}
