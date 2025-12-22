package umc.catchy.domain.group.dto.response;

import umc.catchy.domain.group.domain.Groups;

import java.time.LocalDateTime;

public record GroupCalendarResponse(
        Long groupId,
        String groupName,
        LocalDateTime promiseTime
) {
    public static GroupCalendarResponse from(Groups group) {
        return new GroupCalendarResponse(
                group.getId(),
                group.getGroupName(),
                group.getPromiseTime()
        );
    }
}
