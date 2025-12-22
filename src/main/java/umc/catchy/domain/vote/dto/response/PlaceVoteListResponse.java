package umc.catchy.domain.vote.dto.response;

import java.util.List;

public record PlaceVoteListResponse(String groupLocation, List<PlaceVoteInfo> places, Boolean isLast) {

    public record PlaceVoteInfo(
            Long placeId,
            String placeName,
            String roadAddress,
            double rating,
            long reviewCount,
            String imageUrl,
            List<CategoryVoteResultResponse.VotedMemberInfo> votedMembers
    ) {
        public static PlaceVoteInfo of(Long placeId, String placeName, String roadAddress, double rating,
                                       long reviewCount, String imageUrl, List<CategoryVoteResultResponse.VotedMemberInfo> votedMembers) {
            return new PlaceVoteInfo(placeId, placeName, roadAddress, rating, reviewCount, imageUrl, votedMembers);
        }
    }

    public static PlaceVoteListResponse of(String groupLocation, List<PlaceVoteInfo> places, Boolean isLast) {
        return new PlaceVoteListResponse(groupLocation, places, isLast);
    }
}
