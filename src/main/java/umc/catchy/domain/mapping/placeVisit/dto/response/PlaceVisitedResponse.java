package umc.catchy.domain.mapping.placeVisit.dto.response;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlaceVisitedResponse {
    private Long placeVisitId;
    private LocalDate visitedDate;
    private Boolean isVisited;
}
