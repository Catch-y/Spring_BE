package umc.catchy.domain.vote.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

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
