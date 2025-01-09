package umc.catchy.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "accessToken은 필수 입력 항목입니다.")
        String accessToken
) {
}
