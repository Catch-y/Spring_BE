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
import umc.catchy.domain.mapping.memberGroup.dao.MemberGroupRepository;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.vote.dao.VoteRepository;
import umc.catchy.domain.vote.domain.Vote;
import umc.catchy.domain.vote.dto.request.CreateVoteRequest;
import umc.catchy.domain.vote.dto.response.CategoryDto;
import umc.catchy.domain.vote.dto.response.CategoryResponse;
import umc.catchy.domain.vote.dto.response.GroupVoteStatusResponse;
import umc.catchy.domain.vote.dto.response.MemberVoteStatus;
import umc.catchy.domain.vote.dto.response.VoteResult;
import umc.catchy.domain.vote.dto.response.VoteResultResponse;
import umc.catchy.domain.vote.dto.response.VotedMemberResponse;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository voteRepository;
    private final GroupRepository groupRepository;
    private final CategoryVoteRepository categoryVoteRepository;
    private final MemberCategoryVoteRepository memberCategoryVoteRepository;
    private final MemberRepository memberRepository;
    private final MemberGroupRepository memberGroupRepository;

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

    @Transactional(readOnly = true)
    public VoteResultResponse getVoteResults(Long voteId) {
        Long groupId = categoryVoteRepository.findGroupIdByVoteId(voteId);

        int totalMembers = memberGroupRepository.countByGroupId(groupId);

        List<CategoryVote> categoryVotes = categoryVoteRepository.findByVoteId(voteId);
        List<VoteResult> results = categoryVotes.stream()
                .map(categoryVote -> {
                    List<Member> votedMembers = memberCategoryVoteRepository.findMembersByCategoryVoteId(categoryVote.getId());

                    return new VoteResult(
                            categoryVote.getBigCategory().toString(),
                            votedMembers.size(),
                            votedMembers.stream()
                                    .map(member -> new VotedMemberResponse(member.getId(), member.getNickname(), member.getProfileImage()))
                                    .toList(),
                            0
                    );
                })
                .sorted(Comparator.comparing(VoteResult::getVoteCount).reversed())
                .toList();

        int rank = 1;
        for (int i = 0; i < results.size(); i++) {
            if (i > 0 && results.get(i).getVoteCount() < results.get(i - 1).getVoteCount()) {
                rank = i + 1;
            }
            results.get(i).setRank(rank);
        }

        return new VoteResultResponse(totalMembers, results);
    }

    @Transactional(readOnly = true)
    public GroupVoteStatusResponse getGroupVoteStatus(Long groupId, Long voteId) {
        List<Member> groupMembers = memberGroupRepository.findMembersByGroupId(groupId);

        List<MemberVoteStatus> memberStatuses = groupMembers.stream()
                .map(member -> {
                    boolean hasVoted = memberCategoryVoteRepository.existsByVoteIdAndMemberId(voteId, member.getId());
                    return new MemberVoteStatus(
                            member.getId(),
                            member.getNickname(),
                            member.getProfileImage(),
                            hasVoted
                    );
                })
                .toList();

        return new GroupVoteStatusResponse(groupMembers.size(), memberStatuses);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoriesByVoteId(Long voteId) {
        List<CategoryVote> categories = categoryVoteRepository.findByVoteId(voteId);

        List<CategoryDto> categoryDtos = categories.stream()
                .map(category -> new CategoryDto(category.getId(), category.getBigCategory().toString()))
                .toList();

        return new CategoryResponse(voteId, categoryDtos);
    }
}
