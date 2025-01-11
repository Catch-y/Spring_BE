package umc.catchy.domain.group.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupJoinResponse {
    private boolean success;
    private String message;

    public GroupJoinResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}