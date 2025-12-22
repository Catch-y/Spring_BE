package umc.catchy.domain.group.dto.response;

import umc.catchy.domain.group.domain.Groups;

import java.time.LocalDateTime;

public record GroupInfoResponse(
        String groupName,
        String groupLocation,
        LocalDateTime promiseTime,
        String groupImage
) {
    public static GroupInfoResponse from(Groups group) {
        return new GroupInfoResponse(
                group.getGroupName(),
                group.getGroupLocation(),
                group.getPromiseTime(),
                group.getGroupImage()
        );
    }
}
