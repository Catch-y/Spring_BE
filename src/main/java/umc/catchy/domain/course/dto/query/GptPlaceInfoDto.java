package umc.catchy.domain.course.dto.query;

public record GptPlaceInfoDto(
        Long placeId,
        String placeName,
        String placeImage,
        String categoryKey,
        String roadAddress,
        String activeTime,
        double rating,
        int reviewCount
) {
}
