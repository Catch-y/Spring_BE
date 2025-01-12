package umc.catchy.domain.group.dto.response;

import lombok.Getter;
import lombok.Setter;
import umc.catchy.domain.group.domain.Groups;

import java.time.LocalDateTime;

@Getter
@Setter
public class CreateGroupResponse {

    private Long groupId;
    private String groupName;
    private String groupLocation;
    private String groupImage;
    private String inviteCode;
    private LocalDateTime promiseTime;

    public static CreateGroupResponse fromEntity(Groups group) {
        CreateGroupResponse response = new CreateGroupResponse();
        response.setGroupId(group.getId());
        response.setGroupName(group.getGroupName());
        response.setGroupLocation(group.getGroupLocation());
        response.setGroupImage(group.getGroupImage());
        response.setInviteCode(group.getInviteCode());
        response.setPromiseTime(group.getPromiseTime());
        return response;
    }
}