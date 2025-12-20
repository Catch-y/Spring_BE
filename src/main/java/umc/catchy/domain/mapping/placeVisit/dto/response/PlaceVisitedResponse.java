package umc.catchy.domain.mapping.placeVisit.dto.response;

public record PlaceVisitedResponse(
        Long placeVisitId,
        boolean isVisited
) {
    public static PlaceVisitedResponse of(Long placeVisitId, boolean isVisited) {
        return new PlaceVisitedResponse(placeVisitId, isVisited);
    }
}
