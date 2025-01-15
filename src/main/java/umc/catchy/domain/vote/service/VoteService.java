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
        // 1. 해당 투표의 그룹 ID 조회
        Long groupId = categoryVoteRepository.findGroupIdByVoteId(voteId);

        // 2. 전체 그룹원 수 조회 (MemberGroupRepository 사용)
        int totalMembers = memberGroupRepository.countByGroupId(groupId);

        // 3. 카테고리별 투표 데이터 조회
        List<CategoryVote> categoryVotes = categoryVoteRepository.findByVoteId(voteId);
        List<VoteResult> results = categoryVotes.stream()
                .map(categoryVote -> {
                    // 각 카테고리에 투표한 사용자 목록 조회
                    List<Member> votedMembers = memberCategoryVoteRepository.findMembersByCategoryVoteId(categoryVote.getId());

                    return new VoteResult(
                            categoryVote.getBigCategory().toString(),
                            votedMembers.size(),
                            votedMembers.stream()
                                    .map(member -> new VotedMemberResponse(member.getId(), member.getNickname(), member.getProfileImage()))
                                    .toList(),
                            0 // 초기 순위, 정렬 후 설정
                    );
                })
                .sorted(Comparator.comparing(VoteResult::getVoteCount).reversed()) // 득표수 기준으로 정렬
                .toList();

        // 4. 순위 계산
        int rank = 1;
        for (int i = 0; i < results.size(); i++) {
            if (i > 0 && results.get(i).getVoteCount() < results.get(i - 1).getVoteCount()) {
                rank = i + 1;
            }
            results.get(i).setRank(rank);
        }

        return new VoteResultResponse(totalMembers, results);
    }
}
