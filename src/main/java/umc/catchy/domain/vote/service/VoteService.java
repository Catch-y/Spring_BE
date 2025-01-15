package umc.catchy.domain.vote.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.categoryVote.dao.CategoryVoteRepository;
import umc.catchy.domain.categoryVote.domain.CategoryVote;
import umc.catchy.domain.group.dao.GroupRepository;
import umc.catchy.domain.group.domain.Groups;
import umc.catchy.domain.mapping.memberCategoryVote.dao.MemberCategoryVoteRepository;
import umc.catchy.domain.mapping.memberCategoryVote.domain.MemberCategoryVote;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.vote.dao.VoteRepository;
import umc.catchy.domain.vote.domain.Vote;
import umc.catchy.domain.vote.dto.request.CreateVoteRequest;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository voteRepository;
    private final GroupRepository groupRepository;
    private final CategoryVoteRepository categoryVoteRepository;
    private final MemberCategoryVoteRepository memberCategoryVoteRepository;
    private final MemberRepository memberRepository;

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

    @Transactional
    public void submitVote(Long memberId, Long voteId, List<Long> categoryIds) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        for (Long categoryId : categoryIds) {
            CategoryVote categoryVote = categoryVoteRepository.findById(categoryId)
                    .orElseThrow(() -> new GeneralException(ErrorStatus.CATEGORY_NOT_FOUND));

            if (!categoryVote.getVote().getId().equals(voteId)) {
                throw new GeneralException(ErrorStatus.INVALID_CATEGORY_SELECTION);
            }

            MemberCategoryVote memberCategoryVote = new MemberCategoryVote(member, categoryVote, voteId);
            memberCategoryVoteRepository.save(memberCategoryVote);
        }
    }
}
