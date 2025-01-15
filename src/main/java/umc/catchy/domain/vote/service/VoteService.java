package umc.catchy.domain.vote.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.categoryVote.dao.CategoryVoteRepository;
import umc.catchy.domain.categoryVote.domain.CategoryVote;
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
    private final CategoryVoteRepository categoryVoteRepository;

    public VoteService(VoteRepository voteRepository,
                       GroupRepository groupRepository,
                       CategoryVoteRepository categoryVoteRepository) {
        this.voteRepository = voteRepository;
        this.groupRepository = groupRepository;
        this.categoryVoteRepository = categoryVoteRepository;
    }

    @Transactional
    public Vote createVote(CreateVoteRequest request) {
        Groups group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_NOT_FOUND));

        Vote vote = Vote.createVote(group);
        voteRepository.save(vote);

        for (BigCategory category : BigCategory.values()) {
            CategoryVote categoryVote = new CategoryVote(vote, category);
            categoryVoteRepository.save(categoryVote);
        }

        return vote;
    }
}
