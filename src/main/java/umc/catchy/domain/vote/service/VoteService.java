package umc.catchy.domain.vote.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.catchy.domain.group.dao.GroupRepository;
import umc.catchy.domain.group.domain.Groups;
import umc.catchy.domain.vote.dao.VoteRepository;
import umc.catchy.domain.vote.domain.Vote;
import umc.catchy.domain.vote.dto.request.CreateVoteRequest;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;

@Service
public class VoteService {

    private final VoteRepository voteRepository;
    private final GroupRepository groupRepository;

    public VoteService(VoteRepository voteRepository, GroupRepository groupRepository) {
        this.voteRepository = voteRepository;
        this.groupRepository = groupRepository;
    }

    @Transactional
    public Vote createVote(CreateVoteRequest request) {
        Groups group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_NOT_FOUND));

        Vote vote = Vote.createVote(group);
        return voteRepository.save(vote);
    }
}