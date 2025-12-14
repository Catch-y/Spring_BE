package umc.catchy.infra.feign.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoInfoResponse(
        Long id,
        @JsonProperty("kakao_account") KakaoAccount kakaoAccount
) {
    public record KakaoAccount(String email) {}
}
