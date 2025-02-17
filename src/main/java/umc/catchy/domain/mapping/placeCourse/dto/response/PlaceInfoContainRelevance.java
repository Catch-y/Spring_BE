package umc.catchy.domain.mapping.placeCourse.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlaceInfoContainRelevance {
    private PlaceInfoResponse placeInfoResponse;
    private Integer relevanceScore;
}
