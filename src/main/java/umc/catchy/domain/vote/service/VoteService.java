package umc.catchy.domain.vote.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.categoryVote.dao.CategoryVoteRepository;
import umc.catchy.domain.categoryVote.domain.CategoryVote;
import umc.catchy.domain.course.util.LocationUtils;
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
import umc.catchy.domain.placeReview.dao.PlaceReviewRepository;
import umc.catchy.domain.vote.dao.VoteRepository;
import umc.catchy.domain.vote.domain.Vote;
import umc.catchy.domain.vote.domain.VoteStatus;
import umc.catchy.domain.vote.dto.request.VoteCreateRequest;
import umc.catchy.domain.vote.dto.request.PlaceVoteRequest;
import umc.catchy.domain.vote.dto.response.*;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;
import umc.catchy.infra.config.fcm.FCMService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static umc.catchy.global.common.constants.FcmConstants.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteService {

    private final VoteRepository voteRepository;
    private final GroupRepository groupRepository;
    private final CategoryVoteRepository categoryVoteRepository;
    private final MemberCategoryVoteRepository memberCategoryVoteRepository;
    private final MemberRepository memberRepository;
    private final MemberGroupRepository memberGroupRepository;
    private final PlaceRepository placeRepository;
    private final PlaceReviewRepository placeReviewRepository;
    private final MemberPlaceVoteRepository memberPlaceVoteRepository;
    private final FCMService fcmService;

    @Transactional
    public Vote createVote(VoteCreateRequest request) {
        Groups group = groupRepository.findById(request.groupId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_NOT_FOUND));

        List<Member> members = memberGroupRepository.findMembersByGroupId(group.getId());
        List<String> deviceTokenList = members.stream()
                .filter(member -> member.getFcmInfo().getAppAlarm())
                .map(member -> member.getFcmInfo().getFcmToken())
                .toList();

        Vote vote = Vote.create(group);
        voteRepository.save(vote);

        for (BigCategory bigCategory : BigCategory.values()) {
            CategoryVote categoryVote = CategoryVote.builder()
                    .vote(vote)
                    .bigCategory(bigCategory)
                    .build();
            categoryVoteRepository.save(categoryVote);
        }
        fcmService.sendGroupMessageAsync(deviceTokenList, COURSE_UPDATED_MESSAGE_TITLE, GROUP_VOTE_START_MESSAGE_CONTENT);
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

    public CategoryVoteResultResponse getVoteResults(Long voteId) {
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.VOTE_NOT_FOUND));

        int totalMembers = memberGroupRepository.countByGroupId(vote.getGroup().getId());
        List<CategoryVote> categoryVotes = categoryVoteRepository.findByVoteId(voteId);

        List<CategoryVoteResultResponse.VoteResultInfo> rawResults = categoryVotes.stream()
                .map(cv -> CategoryVoteResultResponse.VoteResultInfo.of(
                        cv.getBigCategory().toString(),
                        memberCategoryVoteRepository.countByVoteIdAndCategoryVoteId(voteId, cv.getId()),
                        memberCategoryVoteRepository.findMembersByCategoryVoteId(cv.getId()).stream()
                                .map(m -> CategoryVoteResultResponse.VotedMemberInfo.of(m.getId(), m.getNickname(), m.getProfileImage()))
                                .toList(),
                        0
                ))
                .sorted(Comparator.comparing(CategoryVoteResultResponse.VoteResultInfo::voteCount).reversed())
                .toList();

        List<CategoryVoteResultResponse.VoteResultInfo> finalResults = new ArrayList<>();
        int rank = 1;
        for (int i = 0; i < rawResults.size(); i++) {
            if (i > 0 && rawResults.get(i).voteCount() < rawResults.get(i - 1).voteCount()) rank = i + 1;
            CategoryVoteResultResponse.VoteResultInfo raw = rawResults.get(i);
            finalResults.add(CategoryVoteResultResponse.VoteResultInfo.of(raw.category(), raw.voteCount(), raw.votedMembers(), rank));
        }

        return CategoryVoteResultResponse.of(vote.getStatus().name(), totalMembers, finalResults);
    }

    @Transactional(readOnly = true)
    public MemberVoteStatusResponse getGroupVoteStatus(Long groupId, Long voteId) {
        Vote vote = voteRepository.findByIdAndGroupId(voteId, groupId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.VOTE_NOT_BELONG_TO_GROUP));

        List<Member> groupMembers = memberGroupRepository.findMembersByGroupId(groupId);

        List<MemberVoteStatusResponse.MemberStatus> memberStatuses = groupMembers.stream()
                .map(member -> {
                    boolean hasVoted = memberCategoryVoteRepository.existsByVoteIdAndMemberId(voteId, member.getId());
                    return MemberVoteStatusResponse.MemberStatus.of(
                            member.getId(),
                            member.getNickname(),
                            member.getProfileImage(),
                            hasVoted
                    );
                })
                .toList();

        return MemberVoteStatusResponse.of(groupMembers.size(), memberStatuses);
    }

    @Transactional(readOnly = true)
    public CategoryVoteListResponse getCategoriesByVoteId(Long voteId) {
        List<CategoryVoteListResponse.CategoryInfo> categoryInfos = categoryVoteRepository.findByVoteId(voteId).stream()
                .map(category -> new CategoryVoteListResponse.CategoryInfo(category.getId(), category.getBigCategory().toString()))
                .toList();

        return CategoryVoteListResponse.of(voteId, categoryInfos);
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
        Vote vote = voteRepository.findByIdAndGroupId(voteId, groupId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.VOTE_NOT_BELONG_TO_GROUP));

        Groups group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_NOT_FOUND));

        List<Member> members = memberGroupRepository.findMembersByGroupId(group.getId());
        List<String> deviceTokenList = members.stream()
                .filter(member -> member.getFcmInfo().getAppAlarm())
                .map(member -> member.getFcmInfo().getFcmToken())
                .collect(Collectors.toList());
        fcmService.sendGroupMessageAsync(deviceTokenList,COURSE_UPDATED_MESSAGE_TITLE,GROUP_VOTE_END_MESSAGE_CONTENT);

        String groupLocation = group.getGroupLocation();
        String alternativeLocation = LocationUtils.normalizeLocation(groupLocation);

        int totalMembers = memberGroupRepository.countByGroupId(groupId);
        int majorityThreshold = (int) Math.ceil(totalMembers / 2.0);

        List<GroupVoteResultResponse.CategoryResult> categories = categoryVoteRepository.findByVoteId(voteId).stream()
                .map(categoryVote -> {
                    int votesForCategory = memberCategoryVoteRepository.countByVoteIdAndCategoryVoteId(voteId, categoryVote.getId());

                    if (votesForCategory >= majorityThreshold) {
                        List<Place> places = placeRepository.findByBigCategoryAndLocation(categoryVote.getBigCategory(), groupLocation, alternativeLocation);
                        return GroupVoteResultResponse.CategoryResult.of(categoryVote.getBigCategory().toString(), places.size());
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();

        return GroupVoteResultResponse.of(groupLocation, categories);
    }

    @Transactional(readOnly = true)
    public PlaceVoteListResponse getPlacesByCategory(Long groupId, String category, int pageSize, Long lastPlaceId) {
        Groups group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_NOT_FOUND));
        String groupLocation = group.getGroupLocation();

        String normalizedGroupLocation = LocationUtils.normalizeLocation(groupLocation);
        String normalizedAlternativeLocation = LocationUtils.normalizeLocation(normalizedGroupLocation);

        Slice<Place> placesSlice = placeRepository.getPlacesByCategoryWithPaging(
                BigCategory.valueOf(category),
                normalizedGroupLocation,
                normalizedAlternativeLocation,
                pageSize,
                lastPlaceId,
                groupId
        );

        List<PlaceVoteListResponse.PlaceVoteInfo> placeInfos = placesSlice.getContent().stream()
                .map(place -> {
                    long reviewCount = placeReviewRepository.countByPlaceId(place.getId());

                    List<Member> votingMembers = memberPlaceVoteRepository.findMembersByPlaceIdAndGroupId(place.getId(), groupId);

                    List<CategoryVoteResultResponse.VotedMemberInfo> votedMembers = votingMembers.stream()
                            .map(member -> CategoryVoteResultResponse.VotedMemberInfo.of(
                                    member.getId(),
                                    member.getNickname(),
                                    member.getProfileImage()
                            ))
                            .toList();

                    return PlaceVoteListResponse.PlaceVoteInfo.of(
                            place.getId(),
                            place.getPlaceName(),
                            place.getRoadAddress(),
                            place.getRating(),
                            reviewCount,
                            place.getImageUrl(),
                            votedMembers
                    );
                })
                .toList();

        boolean isLast = !placesSlice.hasNext();

        return PlaceVoteListResponse.of(groupLocation, placeInfos, isLast);
    }

    @Transactional
    public String togglePlaceVote(Long voteId, Long groupId, PlaceVoteRequest request) {
        Long placeId = request.placeId();

        Member member = memberRepository.findById(SecurityUtil.getCurrentMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PLACE_NOT_FOUND));

        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.VOTE_NOT_FOUND));

        Groups group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_NOT_FOUND));

        MemberPlaceVote existingVote = memberPlaceVoteRepository.findByMemberIdAndPlaceIdAndVoteId(member.getId(), place.getId(), vote.getId());

        if (existingVote != null) {
            memberPlaceVoteRepository.delete(existingVote);
            return "Vote removed successfully.";
        } else {
            MemberPlaceVote memberPlaceVote = MemberPlaceVote.create(place, member, vote, group);
            memberPlaceVoteRepository.save(memberPlaceVote);
            return "Vote added successfully.";
        }
    }

    @Transactional
    public void revoteCategory(Long voteId, List<Long> categoryIds) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        if (categoryIds == null || categoryIds.size() < 2) {
            throw new GeneralException(ErrorStatus.INVALID_CATEGORY_SELECTION);
        }

        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.VOTE_NOT_FOUND));

        if (vote.getStatus() == VoteStatus.COMPLETED) {
            throw new GeneralException(ErrorStatus.VOTE_ALREADY_COMPLETED);
        }

        memberCategoryVoteRepository.deleteByVoteIdAndMemberId(voteId, memberId);

        for (Long categoryId : categoryIds) {
            CategoryVote categoryVote = categoryVoteRepository.findById(categoryId)
                    .orElseThrow(() -> new GeneralException(ErrorStatus.CATEGORY_NOT_FOUND));

            if (!categoryVote.getVote().getId().equals(voteId)) {
                throw new GeneralException(ErrorStatus.INVALID_CATEGORY_SELECTION);
            }

            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

            MemberCategoryVote memberCategoryVote = new MemberCategoryVote(member, categoryVote, voteId);
            memberCategoryVoteRepository.save(memberCategoryVote);
        }

        checkAndUpdateVoteCompletion(voteId);
    }
}
