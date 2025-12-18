package umc.catchy.domain.course.dto.response;

public record GptPlaceInfoResponse(
        Long placeId,
        String placeName,
        String placeImage,
        String category,
        String roadAddress,
        String activeTime,
        double rating,
        int reviewCount
) {
}
