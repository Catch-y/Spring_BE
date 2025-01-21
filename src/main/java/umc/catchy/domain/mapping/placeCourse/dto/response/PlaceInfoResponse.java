package umc.catchy.domain.mapping.placeCourse.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class PlaceInfoResponse {
    private Long placeId;
    private Long poiId;
    private String imageUrl;
    private String placeName;
    private String placeDescription;
    private String category;
    private String roadAddress;
    private String activeTime;
    private Double rating;
    private Long reviewCount;
    private String placeSite;
}
