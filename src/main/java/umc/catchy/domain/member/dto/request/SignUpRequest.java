package umc.catchy.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record SignUpRequest(
        @NotBlank(message = "accessToken은 필수 입력 항목입니다.")
        String accessToken,

        @Schema(description = "애플 회원가입 시에만 필요한 항목입니다.")
        String authorizationCode,

        @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
        @Size(min = 1, max = 8, message = "닉네임은 1자 이상 8자 이하여야합니다.")
        String nickname
) {

}
