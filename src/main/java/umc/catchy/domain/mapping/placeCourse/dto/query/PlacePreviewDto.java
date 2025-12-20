package umc.catchy.domain.mapping.placeCourse.dto.query;

import lombok.Getter;

@Getter
public class PlacePreviewDto {
    private final Long placeId;
    private final String placeName;
    private final String placeImage;
    private final String category;
    private final String roadAddress;
    private final String activeTime;
    private final Double rating;
    private final Double placeLatitude;
    private final Double placeLongitude;
    private final Long reviewCount;
    private final boolean isLiked;

    public PlacePreviewDto(Long placeId, String placeName, String placeImage, String category,
                           String roadAddress, String activeTime, Double rating,
                           Double placeLatitude, Double placeLongitude, Long reviewCount, boolean isLiked) {
        this.placeId = placeId;
        this.placeName = placeName;
        this.placeImage = placeImage;
        this.category = category;
        this.roadAddress = roadAddress;
        this.activeTime = activeTime;
        this.rating = rating != null ? rating : 0.0;
        this.placeLatitude = placeLatitude;
        this.placeLongitude = placeLongitude;
        this.reviewCount = reviewCount;
        this.isLiked = isLiked;
    }
}
