package umc.catchy.domain.group.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InviteCodeRequest {
    @NotBlank(message = "Invite code must not be blank.")
    private String inviteCode;
}