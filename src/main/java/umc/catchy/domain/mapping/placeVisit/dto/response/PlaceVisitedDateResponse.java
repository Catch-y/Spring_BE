package umc.catchy.domain.mapping.placeVisit.dto.response;

import java.time.LocalDate;
import java.util.List;

public record PlaceVisitedDateResponse(
        List<LocalDate> visitedDate
) {
    public static PlaceVisitedDateResponse of(List<LocalDate> visitedDate) {
        return new PlaceVisitedDateResponse(visitedDate);
    }
}
