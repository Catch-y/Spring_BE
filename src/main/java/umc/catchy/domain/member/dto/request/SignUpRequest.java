package umc.catchy.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;


public record SignUpRequest(
        @NotBlank(message = "accessToken은 필수 입력 항목입니다.")
        String accessToken,

        @Schema(description = "애플 회원가입 시에만 필요한 항목입니다.")
        String authorizationCode,

        @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
        String nickname
) {

}
