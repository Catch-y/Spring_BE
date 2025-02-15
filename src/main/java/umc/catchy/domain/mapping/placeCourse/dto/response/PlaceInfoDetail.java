package umc.catchy.domain.mapping.placeCourse.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class PlaceInfoDetail {
    private Long placeId;
    private String imageUrl;
    private String placeName;
    private String placeDescription;
    private String categoryName;
    private String roadAddress;
    private String activeTime;
    private String placeSite;
    private Double rating;
    private Long reviewCount;
    private boolean visited;
    private boolean liked;
}
