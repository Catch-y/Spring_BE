package umc.catchy.domain.group.dto.response;

import umc.catchy.domain.group.domain.Groups;

import java.time.LocalDateTime;

public record CreateGroupResponse(
        Long groupId,
        String groupName,
        String groupLocation,
        String groupImage,
        String inviteCode,
        LocalDateTime promiseTime,
        String creatorNickname
) {
    public static CreateGroupResponse of(Groups group, String creatorNickname) {
        return new CreateGroupResponse(
                group.getId(),
                group.getGroupName(),
                group.getGroupLocation(),
                group.getGroupImage(),
                group.getInviteCode(),
                group.getPromiseTime(),
                creatorNickname
        );
    }
}
