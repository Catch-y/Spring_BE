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
import umc.catchy.domain.mapping.memberPlaceVote.dao.MemberPlaceVoteRepository;
import umc.catchy.domain.mapping.memberPlaceVote.domain.MemberPlaceVote;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.dao.PlaceRepository;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.domain.vote.dao.VoteRepository;
import umc.catchy.domain.vote.domain.Vote;
import umc.catchy.domain.vote.dto.request.PlaceVoteRequest;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VoteCommandService {

    private final VoteRepository voteRepository;
    private final GroupRepository groupRepository;
    private final CategoryVoteRepository categoryVoteRepository;
    private final MemberCategoryVoteRepository memberCategoryVoteRepository;
    private final MemberRepository memberRepository;
    private final MemberGroupRepository memberGroupRepository;
    private final MemberPlaceVoteRepository memberPlaceVoteRepository;
    private final PlaceRepository placeRepository;

    public Vote createVote(Long groupId) {
        Groups group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_NOT_FOUND));
        Vote vote = Vote.create(group);
        Vote savedVote = voteRepository.save(vote);

        for (BigCategory bigCategory : BigCategory.values()) {
            categoryVoteRepository.save(CategoryVote.create(savedVote, bigCategory));
        }

        return savedVote;
    }

    public void submitVote(Long voteId, List<Long> categoryIds, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        if (memberCategoryVoteRepository.existsByVoteIdAndMemberId(voteId, memberId)) {
            throw new GeneralException(ErrorStatus.CATEGORY_ALREADY_VOTED);
        }

        for (Long categoryId : categoryIds) {
            CategoryVote categoryVote = categoryVoteRepository.findById(categoryId)
                    .orElseThrow(() -> new GeneralException(ErrorStatus.CATEGORY_NOT_FOUND));
            memberCategoryVoteRepository.save(MemberCategoryVote.create(member, categoryVote, voteId));
        }
        checkAndUpdateVoteCompletion(voteId);
    }

    public void revoteCategory(Long voteId, List<Long> categoryIds, Long memberId) {
        memberCategoryVoteRepository.deleteByVoteIdAndMemberId(voteId, memberId);
        submitVote(voteId, categoryIds, memberId);
    }

    public void checkAndUpdateVoteCompletion(Long voteId) {
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.VOTE_NOT_FOUND));

        int totalMembers = memberGroupRepository.countByGroupId(vote.getGroup().getId());
        int membersWhoVoted = memberCategoryVoteRepository.countDistinctMembersByVoteId(voteId);

        if (vote.isAllMembersVoted(totalMembers, membersWhoVoted)) {
            vote.complete();
        }
    }

    public String togglePlaceVote(Long voteId, Long groupId, PlaceVoteRequest request, Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
        Place place = placeRepository.findById(request.placeId()).orElseThrow(() -> new GeneralException(ErrorStatus.PLACE_NOT_FOUND));
        Vote vote = voteRepository.findById(voteId).orElseThrow(() -> new GeneralException(ErrorStatus.VOTE_NOT_FOUND));
        Groups group = groupRepository.findById(groupId).orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_NOT_FOUND));
        MemberPlaceVote existingVote = memberPlaceVoteRepository.findByMemberIdAndPlaceIdAndVoteId(memberId, request.placeId(), voteId);

        if (existingVote != null) {
            memberPlaceVoteRepository.delete(existingVote);
            return "Vote removed successfully.";
        } else {
            memberPlaceVoteRepository.save(MemberPlaceVote.create(place, member, vote, group));
            return "Vote added successfully.";
        }
    }
}
