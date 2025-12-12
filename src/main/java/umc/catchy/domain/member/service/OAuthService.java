package umc.catchy.domain.member.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jwt.ReadOnlyJWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import umc.catchy.domain.member.domain.SocialType;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.config.auth.AuthConfig;
import umc.catchy.global.error.exception.GeneralException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.PrivateKey;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
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

    public SocialUserInfo getUserInfo(String token, SocialType socialType) {
        return switch (socialType) {
            case KAKAO -> getKakaoInfo(token);
            case APPLE -> getAppleInfo(token);
        };
    }

    public String getKakaoAccessToken(String code) {
        String reqURL = authConfig.KAKAO_TOKEN_URL;

        try {
            URL url = new URL(reqURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            sendKakaoTokenRequest(conn, code);
            validateResponse(conn);

            return parseKakaoAccessToken(conn);
        } catch (IOException e) {
            throw new GeneralException(ErrorStatus.SOCIAL_MEMBER_NOT_FOUND);
        }
    }

    public void appleWithdraw(String authorizationCode)
            throws IOException, net.minidev.json.parser.ParseException {
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObj = (JSONObject) jsonParser.parse(generateAuthToken(authorizationCode));

        String accessToken = String.valueOf(jsonObj.get("access_token"));

        if (accessToken != null) {
            revokeAppleToken(accessToken);
        }
    }

    public String generateAuthToken(String code) throws IOException {
        if (code == null) {
            throw new GeneralException(ErrorStatus.AUTHORIZATION_CODE_NOT_FOUND);
        }

        MultiValueMap<String, String> params = createAppleTokenParams(code);
        HttpHeaders headers = createJsonHeaders();
        HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(params, headers);

        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(
                    authConfig.APPLE_REQUEST_URL + "/auth/token",
                    HttpMethod.POST,
                    httpEntity,
                    String.class
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new GeneralException(ErrorStatus.AUTHORIZATION_CODE_UNAUTHORIZED);
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

    private SocialUserInfo getKakaoInfo(String token) {
        String postURL = authConfig.KAKAO_INFO_URL;

        try {
            URL url = new URL(postURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + token);

            String result = readResponse(conn);

            JsonElement element = JsonParser.parseString(result);
            String providerId = String.valueOf(element.getAsJsonObject().get("id"));

            JsonObject kakaoAccount = element.getAsJsonObject().get("kakao_account").getAsJsonObject();
            String email = kakaoAccount.get("email").getAsString();

            return new SocialUserInfo(providerId, email);
        } catch (IOException e) {
            throw new GeneralException(ErrorStatus.SOCIAL_MEMBER_NOT_FOUND);
        }
    }

    private void sendKakaoTokenRequest(HttpURLConnection conn, String code) throws IOException {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
        StringBuilder sb = new StringBuilder();
        sb.append("grant_type=").append(GRANT_TYPE_AUTHORIZATION_CODE);
        sb.append("&client_id=").append(authConfig.KAKAO_CLIENT_ID);
        sb.append("&client_secret=").append(authConfig.KAKAO_CLIENT_SECRET);
        sb.append("&redirect_uri=").append(authConfig.KAKAO_REDIRECT_URL);
        sb.append("&code=").append(code);
        bw.write(sb.toString());
        bw.flush();
        bw.close();
    }

    private void validateResponse(HttpURLConnection conn) throws IOException {
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            readErrorResponse(conn);
            throw new GeneralException(ErrorStatus.SOCIAL_MEMBER_NOT_FOUND);
        }
    }

    private String readErrorResponse(HttpURLConnection conn) throws IOException {
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        StringBuilder errorResult = new StringBuilder();
        String errorLine;
        while ((errorLine = errorReader.readLine()) != null) {
            errorResult.append(errorLine);
        }
        errorReader.close();
        return errorResult.toString();
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            result.append(line);
        }
        br.close();
        return result.toString();
    }

    private String parseKakaoAccessToken(HttpURLConnection conn) throws IOException {
        String result = readResponse(conn);
        JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(result);
        return element.getAsJsonObject().get("access_token").getAsString();
    }

    private void revokeAppleToken(String accessToken) {
        RestTemplate restTemplate = new RestTemplateBuilder().build();
        String revokeUrl = authConfig.APPLE_REQUEST_URL + "/auth/oauth2/v2/revoke";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", authConfig.APPLE_CLIENT_ID);
        try {
            params.add("client_secret", createClientSecret());
        } catch (IOException e) {
            throw new GeneralException(ErrorStatus.AUTHORIZATION_CODE_UNAUTHORIZED);
        }
        params.add("token", accessToken);

        HttpHeaders headers = createJsonHeaders();
        HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(params, headers);

        restTemplate.postForEntity(revokeUrl, httpEntity, String.class);
    }

    private MultiValueMap<String, String> createAppleTokenParams(String code) throws IOException {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", GRANT_TYPE_AUTHORIZATION_CODE);
        params.add("client_id", authConfig.APPLE_CLIENT_ID);
        params.add("client_secret", createClientSecret());
        params.add("code", code);
        params.add("redirect_uri", authConfig.APPLE_REDIRECT_URL);
        return params;
    }

    private HttpHeaders createJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
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
