package umc.catchy.domain.mapping.placeLike.dto.response;

public record PlaceLikedResponse(
        Long placeLikeId,
        boolean liked
) {
    public static PlaceLikedResponse of(Long placeLikeId, boolean liked) {
        return new PlaceLikedResponse(placeLikeId, liked);
    }
}
