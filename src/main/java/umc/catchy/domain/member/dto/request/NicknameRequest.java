package umc.catchy.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NicknameRequest(
        @NotBlank(message = "변경할 닉네임을 입력해주세요.")
        @Size(min = 1, max = 8, message = "닉네임은 1자 이상 8자 이하여야합니다.")
        String nickname
) {
}
