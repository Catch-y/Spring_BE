package umc.catchy.domain.vote.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class GroupPlaceResponse {
    private String groupLocation;
    private List<PlaceResponse> places;
}
