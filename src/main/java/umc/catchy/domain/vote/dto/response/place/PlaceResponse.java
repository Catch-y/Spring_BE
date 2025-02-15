package umc.catchy.domain.vote.dto.response.place;

import lombok.AllArgsConstructor;
import lombok.Getter;
import umc.catchy.domain.vote.dto.response.vote.VotedMemberResponse;

import java.util.List;

@Getter
@AllArgsConstructor
public class PlaceResponse {
    private Long placeId;
    private String placeName;
    private String roadAddress;
    private double rating;
    private long reviewCount;
    private String imageUrl;
    private List<VotedMemberResponse> votedMembers;
}
