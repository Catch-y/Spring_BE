package umc.catchy.domain.vote.dto.response;

import java.util.List;

public record CategoryVoteResultResponse(String status, int totalMembers, List<VoteResultInfo> results) {
    public record VoteResultInfo(
            String category,
            int voteCount,
            List<VotedMemberInfo> votedMembers,
            int rank
    ) {
        public static VoteResultInfo of(String category, int voteCount, List<VotedMemberInfo> votedMembers, int rank) {
            return new VoteResultInfo(category, voteCount, votedMembers, rank);
        }
    }

    public record VotedMemberInfo(Long memberId, String nickname, String profileImage) {
        public static VotedMemberInfo of(Long memberId, String nickname, String profileImage) {
            return new VotedMemberInfo(memberId, nickname, profileImage);
        }
    }

    public static CategoryVoteResultResponse of(String status, int totalMembers, List<VoteResultInfo> results) {
        return new CategoryVoteResultResponse(status, totalMembers, results);
    }
}
