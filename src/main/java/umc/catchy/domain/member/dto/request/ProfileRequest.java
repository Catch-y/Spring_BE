package umc.catchy.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ProfileRequest(
        @NotBlank(message = "변경할 닉네임을 입력해주세요")
        String nickname
) {
}
