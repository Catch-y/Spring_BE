package umc.catchy.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import umc.catchy.global.common.constants.RegexConstants;

public record SignUpRequest(
        @NotNull(message = "유저 ID는 필수 입력 항목입니다.")
        Long providerId,

        @NotBlank(message = "이메일은 필수 입력 항목입니다.")
        @Email
        @Pattern(regexp = RegexConstants.EMAIL_REGEX, message = "유효한 이메일 주소여야 합니다.")
        @Schema(example = "catch@example.com", description = "유효한 이메일 주소")
        String email,

        @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
        String nickname
) {

}
