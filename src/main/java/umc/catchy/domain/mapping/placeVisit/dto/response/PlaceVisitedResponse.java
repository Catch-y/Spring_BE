package umc.catchy.domain.mapping.placeVisit.dto.response;

import java.time.LocalDate;

public record PlaceVisitedResponse(
        Long placeVisitId,
        LocalDate visitedDate,
        boolean isVisited
) {
    public static PlaceVisitedResponse of(Long placeVisitId, LocalDate visitedDate, boolean isVisited) {
        return new PlaceVisitedResponse(placeVisitId, visitedDate, isVisited);
    }
}
