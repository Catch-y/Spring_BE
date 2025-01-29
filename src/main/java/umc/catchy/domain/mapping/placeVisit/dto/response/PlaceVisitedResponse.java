package umc.catchy.domain.mapping.placeVisit.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlaceVisitedResponse {
    private Long placeVisitId;
    private LocalDateTime visitedDate;
    private Boolean isVisited;
}
