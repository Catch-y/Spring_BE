package umc.catchy.domain.group.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class GroupInfoResponse {
    private String groupName;
    private String groupLocation;
    private LocalDateTime promiseTime;
    private String groupImage;
}