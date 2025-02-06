package umc.catchy.domain.mapping.placeCourse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PlaceInfoPreview {
    private Long placeId;
    private String placeName;
    private String placeImage;
    private String category;
    private String roadAddress;
    private String activeTime;
    private Double rating;
    private Long reviewCount;
    private boolean isLiked;
}
