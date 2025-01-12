package umc.catchy.domain.group.dto.response;

import lombok.Builder;
import lombok.Getter;
import umc.catchy.domain.group.domain.Groups;

import java.time.LocalDateTime;

@Getter
@Builder
public class CreateGroupResponse {

    private Long groupId;
    private String groupName;
    private String groupLocation;
    private String groupImage;
    private String inviteCode;
    private LocalDateTime promiseTime;
    private String creatorNickname;

    public static CreateGroupResponse fromEntity(Groups group, String creatorNickname) {
        return CreateGroupResponse.builder()
                .groupId(group.getId())
                .groupName(group.getGroupName())
                .groupLocation(group.getGroupLocation())
                .groupImage(group.getGroupImage())
                .inviteCode(group.getInviteCode())
                .promiseTime(group.getPromiseTime())
                .creatorNickname(creatorNickname)
                .build();
    }
}