package umc.catchy.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;


public record SignUpRequest(
        @NotBlank(message = "accessToken은 필수 입력 항목입니다.")
        String accessToken,

        @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
        String nickname
) {

}
