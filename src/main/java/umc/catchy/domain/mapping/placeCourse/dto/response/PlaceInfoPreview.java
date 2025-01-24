package umc.catchy.domain.mapping.placeCourse.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
public class PlaceInfoPreview {
    private Long placeId;
    private String placeName;
    private String category;
    private String roadAddress;
    private String activeTime;
    private Double rating;
    private Long reviewCount;
}
