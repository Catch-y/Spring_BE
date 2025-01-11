package umc.catchy.domain.group.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InviteCodeRequest {
    private String inviteCode;
    private Long memberId;
}