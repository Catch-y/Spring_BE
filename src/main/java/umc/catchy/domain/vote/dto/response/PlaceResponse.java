package umc.catchy.domain.vote.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PlaceResponse {
    private Long placeId;
    private String placeName;
    private String roadAddress;
    private double rating;
    private long reviewCount;
}
