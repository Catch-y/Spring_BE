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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class OAuthService {

    private final AuthConfig authConfig;

    public SocialUserInfo getUserInfo(String token, SocialType socialType) {
        return switch (socialType) {
            case KAKAO -> getKakaoInfo(token);
            case APPLE -> getAppleInfo(token);
        };
    }

    /* 카카오 액세스 토큰 발급 */
    public String getKakaoAccessToken (String code) {
        String access_Token = "";
        String refresh_Token = "";
        String reqURL = authConfig.KAKAO_TOKEN_URL;

        try {
            URL url = new URL(reqURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            //POST 요청을 위해 기본값이 false인 setDoOutput을 true로
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            //POST 요청에 필요로 요구하는 파라미터 스트림을 통해 전송
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
            StringBuilder sb = new StringBuilder();
            sb.append("grant_type=authorization_code");
            sb.append("&client_id=" + authConfig.KAKAO_CLIENT_ID);
            sb.append("&client_secret=" + authConfig.KAKAO_CLIENT_SECRET);
            sb.append("&redirect_uri=" + authConfig.KAKAO_REDIRECT_URL);
            sb.append("&code=" + code);
            bw.write(sb.toString());
            bw.flush();

            //결과 코드가 200이라면 성공
            int responseCode = conn.getResponseCode();

            if (responseCode != 200) {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                String errorLine = "";
                StringBuilder errorResult = new StringBuilder();
                while ((errorLine = errorReader.readLine()) != null) {
                    errorResult.append(errorLine);
                }
                errorReader.close();
            }

            //요청을 통해 얻은 JSON타입의 Response 메세지 읽어오기
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = "";
            String result = "";

            while ((line = br.readLine()) != null) {
                result += line;
            }

            //Gson 라이브러리에 포함된 클래스로 JSON파싱 객체 생성
            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(result);

            access_Token = element.getAsJsonObject().get("access_token").getAsString();
            refresh_Token = element.getAsJsonObject().get("refresh_token").getAsString();

            br.close();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return access_Token;
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
            log.error("Kakao user info request failed", e);
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

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = "";
            StringBuilder result = new StringBuilder();

            while ((line = br.readLine()) != null) {
                result.append(line);
            }

            JsonElement element = JsonParser.parseString(result.toString());
            String providerId = String.valueOf(element.getAsJsonObject().get("id"));

            JsonObject kakaoAccount = element.getAsJsonObject().get("kakao_account").getAsJsonObject();
            String email = kakaoAccount.getAsJsonObject().get("email").getAsString();

            return new SocialUserInfo(providerId, email);
        } catch (IOException e) {
            log.error("Apple user info request failed", e);
            throw new GeneralException(ErrorStatus.SOCIAL_MEMBER_NOT_FOUND);
        }
    }

    public void appleWithdraw(String authorizationCode)
            throws IOException, net.minidev.json.parser.ParseException {
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObj = (JSONObject) jsonParser.parse(generateAuthToken(authorizationCode));

        String accessToken = String.valueOf(jsonObj.get("access_token"));

        if (accessToken != null) {
            RestTemplate restTemplate = new RestTemplateBuilder().build();
            String revokeUrl = authConfig.APPLE_REQUEST_URL + "/auth/oauth2/v2/revoke";
            LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", authConfig.APPLE_CLIENT_ID);
            params.add("client_secret", createClientSecret());
            params.add("token", accessToken);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(params, headers);
            restTemplate.postForEntity(revokeUrl, httpEntity, String.class);
        }
    }

    public String generateAuthToken(String code) throws IOException {
        if (code == null) throw new GeneralException(ErrorStatus.AUTHORIZATION_CODE_NOT_FOUND);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", authConfig.APPLE_CLIENT_ID);
        params.add("client_secret", createClientSecret());
        params.add("code", code);
        params.add("redirect_uri", authConfig.APPLE_REDIRECT_URL);

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(params, headers);

        try {
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

    private String createClientSecret() throws IOException {
        Date expirationDate = Date.from(LocalDateTime.now().plusDays(30).atZone(ZoneId.systemDefault()).toInstant());
        Map<String, Object> jwtHeader = new HashMap<>();
        jwtHeader.put("kid", authConfig.APPLE_KEY_ID);
        jwtHeader.put("alg", "ES256"); // alg

        return Jwts.builder()
                .setHeaderParams(jwtHeader)
                .setIssuer(authConfig.APPLE_TEAM_ID) // iss
                .setIssuedAt(new Date(System.currentTimeMillis())) // 발행 시간
                .setExpiration(expirationDate) // 만료 시간
                .setAudience(authConfig.APPLE_REQUEST_URL) // aud
                .setSubject(authConfig.APPLE_CLIENT_ID) // sub
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
