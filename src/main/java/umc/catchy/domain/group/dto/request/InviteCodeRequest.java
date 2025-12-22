package umc.catchy.domain.group.dto.request;

import jakarta.validation.constraints.NotBlank;

public record InviteCodeRequest(
        @NotBlank(message = "Invite code must not be blank.")
        String inviteCode
) {
}
