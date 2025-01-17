package umc.catchy.domain.vote.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.catchy.domain.category.dao.CategoryRepository;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.category.domain.Category;
import umc.catchy.domain.categoryVote.dao.CategoryVoteRepository;
import umc.catchy.domain.categoryVote.domain.CategoryVote;
import umc.catchy.domain.group.dao.GroupRepository;
import umc.catchy.domain.group.domain.Groups;
import umc.catchy.domain.mapping.memberCategoryVote.dao.MemberCategoryVoteRepository;
import umc.catchy.domain.mapping.memberCategoryVote.domain.MemberCategoryVote;
import umc.catchy.domain.mapping.memberGroup.dao.MemberGroupRepository;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.dao.PlaceRepository;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.domain.vote.dao.VoteRepository;
import umc.catchy.domain.vote.domain.Vote;
import umc.catchy.domain.vote.domain.VoteStatus;
import umc.catchy.domain.vote.dto.request.CreateVoteRequest;
import umc.catchy.domain.vote.dto.response.CategoryDto;
import umc.catchy.domain.vote.dto.response.CategoryResponse;
import umc.catchy.domain.vote.dto.response.CategoryResult;
import umc.catchy.domain.vote.dto.response.GroupVoteResultResponse;
import umc.catchy.domain.vote.dto.response.GroupVoteStatusResponse;
import umc.catchy.domain.vote.dto.response.MemberVoteStatus;
import umc.catchy.domain.vote.dto.response.PlaceResponse;
import umc.catchy.domain.vote.dto.response.VoteResult;
import umc.catchy.domain.vote.dto.response.VoteResultResponse;
import umc.catchy.domain.vote.dto.response.VotedMemberResponse;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository voteRepository;
    private final GroupRepository groupRepository;
    private final CategoryVoteRepository categoryVoteRepository;
    private final MemberCategoryVoteRepository memberCategoryVoteRepository;
    private final MemberRepository memberRepository;
    private final MemberGroupRepository memberGroupRepository;
    private final PlaceRepository placeRepository;
    private final CategoryRepository categoryRepository; // 주입 추가


    @Transactional
    public Vote createVote(CreateVoteRequest request) {
        Groups group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_NOT_FOUND));

        Vote vote = Vote.builder()
                .status(VoteStatus.IN_PROGRESS)
                .endTime(LocalDateTime.now().plusDays(1))
                .group(group)
                .build();

        voteRepository.save(vote);

        for (BigCategory bigCategory : BigCategory.values()) {
            // Category 객체를 데이터베이스에서 조회
            Category category = categoryRepository.findByBigCategory(bigCategory)
                    .orElseThrow(() -> new GeneralException(ErrorStatus.CATEGORY_NOT_FOUND));

            // CategoryVote 생성 시 Category 객체 전달
            CategoryVote categoryVote = new CategoryVote(vote, bigCategory, category);
            categoryVoteRepository.save(categoryVote);
        }

        return vote;
    }

    @Transactional
    public void submitVote(Long voteId, List<Long> categoryIds) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        boolean hasAlreadyVoted = memberCategoryVoteRepository.existsByVoteIdAndMemberId(voteId, memberId);
        if (hasAlreadyVoted) {
            throw new GeneralException(ErrorStatus.CATEGORY_ALREADY_VOTED);
        }

        for (Long categoryId : categoryIds) {
            CategoryVote categoryVote = categoryVoteRepository.findById(categoryId)
                    .orElseThrow(() -> new GeneralException(ErrorStatus.CATEGORY_NOT_FOUND));

            if (!categoryVote.getVote().getId().equals(voteId)) {
                throw new GeneralException(ErrorStatus.INVALID_CATEGORY_SELECTION);
            }

            MemberCategoryVote memberCategoryVote = new MemberCategoryVote(member, categoryVote, voteId);
            memberCategoryVoteRepository.save(memberCategoryVote);
        }

        checkAndUpdateVoteCompletion(voteId);
    }

    @Transactional(readOnly = true)
    public VoteResultResponse getVoteResults(Long voteId) {
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.VOTE_NOT_FOUND));

        Long groupId = vote.getGroup().getId();
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

        return new VoteResultResponse(vote.getStatus().name(), totalMembers, results);
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

    @Transactional
    public void checkAndUpdateVoteCompletion(Long voteId) {
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.VOTE_NOT_FOUND));

        Long groupId = vote.getGroup().getId();
        int totalMembers = memberGroupRepository.countByGroupId(groupId);

        int membersWhoVoted = memberCategoryVoteRepository.countDistinctMembersByVoteId(voteId);

        if (totalMembers == membersWhoVoted) {
            vote.changeStatus(VoteStatus.COMPLETED);
            voteRepository.save(vote);
        }
    }

    @Transactional(readOnly = true)
    public GroupVoteResultResponse getGroupVoteResults(Long groupId, Long voteId) {
        // 그룹 정보 조회
        Groups group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_NOT_FOUND));
        String groupLocation = group.getGroupLocation();

        // 그룹 인원 수 계산
        int totalMembers = memberGroupRepository.countByGroupId(groupId);
        int majorityThreshold = (int) Math.ceil(totalMembers / 2.0);

        // 카테고리별 장소 조회
        List<CategoryResult> categories = categoryVoteRepository.findByVoteId(voteId).stream()
                .map(categoryVote -> {
                    int votesForCategory = memberCategoryVoteRepository.countByVoteIdAndCategoryVoteId(voteId, categoryVote.getId());

                    if (votesForCategory >= majorityThreshold) {
                        List<Place> places = placeRepository.findByBigCategoryAndLocation(categoryVote.getBigCategory(), groupLocation);
                        List<PlaceResponse> placeResponses = places.stream()
                                .map(place -> new PlaceResponse(place.getPlaceName(), place.getRoadAddress(), place.getLatitude(), place.getLongitude()))
                                .toList();

                        return new CategoryResult(categoryVote.getBigCategory().toString(), placeResponses);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();

        return new GroupVoteResultResponse(groupLocation, categories);
    }
}
