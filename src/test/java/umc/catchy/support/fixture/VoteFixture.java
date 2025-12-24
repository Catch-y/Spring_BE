package umc.catchy.support.fixture;

import umc.catchy.domain.group.domain.Groups;
import umc.catchy.domain.vote.domain.Vote;
import umc.catchy.domain.vote.domain.VoteStatus;
import umc.catchy.domain.vote.dto.request.VoteCreateRequest;

import java.time.LocalDateTime;

public class VoteFixture {

    public static VoteCreateRequest createVoteRequest(Long groupId) {
        return new VoteCreateRequest(groupId);
    }

    public static Vote createVote(Groups group) {
        return Vote.builder()
                .status(VoteStatus.IN_PROGRESS)
                .endTime(LocalDateTime.now().plusDays(1))
                .group(group)
                .build();
    }

    public static Vote createVoteWithId(Long id, Groups group) {
        return Vote.builder()
                .id(id)
                .status(VoteStatus.IN_PROGRESS)
                .endTime(LocalDateTime.now().plusDays(1))
                .group(group)
                .build();
    }
}
