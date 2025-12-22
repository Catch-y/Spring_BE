package umc.catchy.domain.vote.dto.response;

import java.util.List;

public record MemberVoteStatusResponse(int totalMembers, List<MemberStatus> members) {

    public record MemberStatus(Long memberId, String nickname, String profileImage, boolean hasVoted) {
        public static MemberStatus of(Long memberId, String nickname, String profileImage, boolean hasVoted) {
            return new MemberStatus(memberId, nickname, profileImage, hasVoted);
        }
    }

    public static MemberVoteStatusResponse of(int totalMembers, List<MemberStatus> members) {
        return new MemberVoteStatusResponse(totalMembers, members);
    }
}
