package umc.catchy.infra.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import umc.catchy.infra.feign.dto.AppleTokenResponse;

@FeignClient(name = "appleClient", url = "https://appleid.apple.com")
public interface AppleFeignClient {

    @PostMapping(value = "/auth/token", consumes = "application/x-www-form-urlencoded")
    AppleTokenResponse getToken(
            @RequestParam("client_id") String clientId,
            @RequestParam("client_secret") String clientSecret,
            @RequestParam("code") String code,
            @RequestParam("grant_type") String grantType,
            @RequestParam("redirect_uri") String redirectUri
    );

    @PostMapping(value = "/auth/oauth2/v2/revoke", consumes = "application/x-www-form-urlencoded")
    void revokeToken(
            @RequestParam("client_id") String clientId,
            @RequestParam("client_secret") String clientSecret,
            @RequestParam("token") String token
    );
}
