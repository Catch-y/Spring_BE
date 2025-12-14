package umc.catchy.infra.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import umc.catchy.infra.feign.dto.KakaoInfoResponse;

@FeignClient(name = "kakaoClient", url = "https://kapi.kakao.com")
public interface KakaoFeignClient {

    @PostMapping("/v2/user/me")
    KakaoInfoResponse getUserInfo(@RequestHeader("Authorization") String accessToken);
}
