package umc.catchy.domain.vote.dto.response.group;

import lombok.AllArgsConstructor;
import lombok.Getter;
import umc.catchy.domain.vote.dto.response.place.VotePlaceResponse;

import java.util.List;

@Getter
@AllArgsConstructor
public class GroupPlaceResponse {
    private String groupLocation;
    private List<VotePlaceResponse> places;
    private Boolean isLast;
}
