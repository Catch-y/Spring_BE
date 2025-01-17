package umc.catchy.domain.vote.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PlaceResponse {
    private String placeName;
    private String roadAddress;
    private Double latitude;
    private Double longitude;
}
