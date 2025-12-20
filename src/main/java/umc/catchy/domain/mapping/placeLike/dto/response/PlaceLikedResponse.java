package umc.catchy.domain.mapping.placeLike.dto.response;

public record PlaceLikedResponse(
        Long placeLikeId,
        boolean isLiked
) {
    public static PlaceLikedResponse of(Long placeLikeId, boolean isLiked) {
        return new PlaceLikedResponse(placeLikeId, isLiked);
    }
}
