package umc.catchy.global.config.auth;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class AuthConfig {

    @Value("${security.kakao.client-id}")
    public String KAKAO_CLIENT_ID;

    @Value("${security.kakao.client-secret}")
    public String KAKAO_CLIENT_SECRET;

    @Value("${security.kakao.redirect-url}")
    public String KAKAO_REDIRECT_URL;

    @Value("${security.kakao.token-request-url}")
    public String KAKAO_TOKEN_URL;

    @Value("${security.kakao.info-request-url}")
    public String KAKAO_INFO_URL;

    @Value("${security.apple.key-id}")
    public String APPLE_KEY_ID;

    @Value("${security.apple.service-id}")
    public String APPLE_CLIENT_ID;

    @Value("${security.apple.team-id}")
    public String APPLE_TEAM_ID;

    @Value("${security.apple.redirect-url}")
    public String APPLE_REDIRECT_URL;

    @Value("${security.apple.request-url}")
    public String APPLE_REQUEST_URL;

    @Value("${security.apple.private-key}")
    public String APPLE_PRIVATE_KEY;
}
