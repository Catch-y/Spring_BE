package umc.catchy.domain.mapping.placeCourse.dto.response;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceInfoResponse {
    private Long placeId;
    private String imageUrl;
    private String placeName;
    private String categoryName;
    private String roadAddress;
    private String activeTime;
    private Double rating;
    private Long reviewCount;
}
