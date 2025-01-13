package umc.catchy.domain.group.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class GroupCalendarResponse {
    private Long groupId;
    private String groupName;
    private LocalDateTime promiseTime;
}
