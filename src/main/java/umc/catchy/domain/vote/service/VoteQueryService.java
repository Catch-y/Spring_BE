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
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.dao.PlaceRepository;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.domain.placeReview.dao.PlaceReviewRepository;
import umc.catchy.domain.vote.dao.VoteRepository;
import umc.catchy.domain.vote.domain.Vote;
import umc.catchy.domain.vote.dto.response.*;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteQueryService {

    private final VoteRepository voteRepository;
    private final GroupRepository groupRepository;
    private final CategoryVoteRepository categoryVoteRepository;
    private final MemberCategoryVoteRepository memberCategoryVoteRepository;
    private final MemberGroupRepository memberGroupRepository;
    private final PlaceRepository placeRepository;
    private final PlaceReviewRepository placeReviewRepository;
    private final MemberPlaceVoteRepository memberPlaceVoteRepository;

    public CategoryVoteResultResponse getVoteResults(Long voteId) {
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.VOTE_NOT_FOUND));

        int totalMembers = memberGroupRepository.countByGroupId(vote.getGroup().getId());
        List<CategoryVote> categoryVotes = categoryVoteRepository.findByVoteId(voteId);

        Map<Long, Long> voteCountMap = memberCategoryVoteRepository.countVotesByVoteIdGroupByCategory(voteId).stream()
                .collect(Collectors.toMap(obj -> (Long) obj[0], obj -> (Long) obj[1]));

        Map<Long, List<Member>> membersByCategoryMap = memberCategoryVoteRepository.findAllByVoteIdWithMember(voteId).stream()
                .collect(Collectors.groupingBy(mcv -> mcv.getCategoryVote().getId(),
                        Collectors.mapping(MemberCategoryVote::getMember, Collectors.toList())));

        List<CategoryVoteResultResponse.VoteResultInfo> rawResults = categoryVotes.stream()
                .map(cv -> {
                    Long count = voteCountMap.getOrDefault(cv.getId(), 0L);
                    List<Member> members = membersByCategoryMap.getOrDefault(cv.getId(), List.of());
                    List<CategoryVoteResultResponse.VotedMemberInfo> votedMemberInfos = members.stream()
                            .map(m -> CategoryVoteResultResponse.VotedMemberInfo.of(m.getId(), m.getNickname(), m.getProfileImage()))
                            .toList();
                    return CategoryVoteResultResponse.VoteResultInfo.of(cv.getBigCategory().toString(), count.intValue(), votedMemberInfos, 0);
                })
                .sorted(Comparator.comparing(CategoryVoteResultResponse.VoteResultInfo::voteCount).reversed())
                .toList();

        return buildRankedResults(vote.getStatus().name(), totalMembers, rawResults);
    }

    public MemberVoteStatusResponse getGroupVoteStatus(Long groupId, Long voteId) {
        if (!voteRepository.existsByIdAndGroupId(voteId, groupId)) {
            throw new GeneralException(ErrorStatus.VOTE_NOT_BELONG_TO_GROUP);
        }

        List<Member> groupMembers = memberGroupRepository.findMembersByGroupId(groupId);
        List<MemberVoteStatusResponse.MemberStatus> memberStatuses = groupMembers.stream()
                .map(member -> MemberVoteStatusResponse.MemberStatus.of(
                        member.getId(), member.getNickname(), member.getProfileImage(),
                        memberCategoryVoteRepository.existsByVoteIdAndMemberId(voteId, member.getId())))
                .toList();

        return MemberVoteStatusResponse.of(groupMembers.size(), memberStatuses);
    }

    public CategoryVoteListResponse getCategoriesByVoteId(Long voteId) {
        List<CategoryVote> categories = categoryVoteRepository.findByVoteId(voteId);
        List<CategoryVoteListResponse.CategoryInfo> categoryInfos = categories.stream()
                .map(cv -> CategoryVoteListResponse.CategoryInfo.of(cv.getId(), cv.getBigCategory().name()))
                .toList();
        return CategoryVoteListResponse.of(voteId, categoryInfos);
    }

    public GroupVoteResultResponse getGroupVoteResults(Long groupId, Long voteId) {
        Groups group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_NOT_FOUND));

        CategoryVoteResultResponse results = getVoteResults(voteId);
        List<GroupVoteResultResponse.CategoryResult> categoryResults = results.results().stream()
                .filter(res -> res.rank() == 1)
                .map(res -> GroupVoteResultResponse.CategoryResult.of(res.category(), res.voteCount()))
                .toList();

        return GroupVoteResultResponse.of(group.getGroupLocation(), categoryResults);
    }

    public PlaceVoteListResponse getPlacesByCategory(Long groupId, BigCategory category, int pageSize, Long lastPlaceId) {
        Groups group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_NOT_FOUND));

        Slice<Place> placesSlice = placeRepository.getPlacesByCategoryWithPaging(
                category, group.getGroupLocation(),
                LocationUtils.normalizeLocation(group.getGroupLocation()), pageSize, lastPlaceId, groupId);

        List<Place> places = placesSlice.getContent();
        List<Long> placeIds = places.stream().map(Place::getId).toList();

        Map<Long, Long> reviewCountMap = placeReviewRepository.countByPlaceIdsGroupByPlace(placeIds).stream()
                .collect(Collectors.toMap(obj -> (Long) obj[0], obj -> (Long) obj[1]));

        Map<Long, List<Member>> votingMembersMap = memberPlaceVoteRepository.findMembersByPlaceIdsAndGroupId(placeIds, groupId).stream()
                .collect(Collectors.groupingBy(mpv -> mpv.getPlace().getId(),
                        Collectors.mapping(MemberPlaceVote::getMember, Collectors.toList())));

        List<PlaceVoteListResponse.PlaceVoteInfo> placeInfos = places.stream()
                .map(place -> PlaceVoteListResponse.PlaceVoteInfo.of(
                        place.getId(), place.getPlaceName(), place.getRoadAddress(), place.getRating(),
                        reviewCountMap.getOrDefault(place.getId(), 0L), place.getImageUrl(),
                        votingMembersMap.getOrDefault(place.getId(), List.of()).stream()
                                .map(m -> CategoryVoteResultResponse.VotedMemberInfo.of(m.getId(), m.getNickname(), m.getProfileImage())).toList()
                )).toList();

        return PlaceVoteListResponse.of(group.getGroupLocation(), placeInfos, !placesSlice.hasNext());
    }

    private CategoryVoteResultResponse buildRankedResults(String status, int totalMembers, List<CategoryVoteResultResponse.VoteResultInfo> rawResults) {
        List<CategoryVoteResultResponse.VoteResultInfo> finalResults = new ArrayList<>();
        int rank = 1;
        for (int i = 0; i < rawResults.size(); i++) {
            if (i > 0 && rawResults.get(i).voteCount() < rawResults.get(i - 1).voteCount()) rank = i + 1;
            CategoryVoteResultResponse.VoteResultInfo raw = rawResults.get(i);
            finalResults.add(CategoryVoteResultResponse.VoteResultInfo.of(raw.category(), raw.voteCount(), raw.votedMembers(), rank));
        }
        return CategoryVoteResultResponse.of(status, totalMembers, finalResults);
    }
}
