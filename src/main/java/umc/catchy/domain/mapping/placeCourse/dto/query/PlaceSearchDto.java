package umc.catchy.domain.mapping.placeCourse.dto.query;

import lombok.Getter;

@Getter
public class PlaceSearchDto {
    private final Long placeId;
    private final String imageUrl;
    private final String placeName;
    private final String categoryName;
    private final String roadAddress;
    private final String activeTime;
    private final Double rating;
    private final Long reviewCount;
    private final Integer relevanceScore;

    public PlaceSearchDto(Long placeId, String imageUrl, String placeName, String categoryName,
                          String roadAddress, String activeTime, Double rating, Long reviewCount,
                          Integer relevanceScore) {
        this.placeId = placeId;
        this.imageUrl = imageUrl;
        this.placeName = placeName;
        this.categoryName = categoryName;
        this.roadAddress = roadAddress;
        this.activeTime = activeTime;
        this.rating = rating != null ? rating : 0.0;
        this.reviewCount = reviewCount;
        this.relevanceScore = relevanceScore;
    }
}
