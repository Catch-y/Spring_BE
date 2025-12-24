package umc.catchy.domain.vote.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.categoryVote.dao.CategoryVoteRepository;
import umc.catchy.domain.categoryVote.domain.CategoryVote;
import umc.catchy.domain.group.dao.GroupRepository;
import umc.catchy.domain.group.domain.Groups;
import umc.catchy.domain.mapping.memberCategoryVote.dao.MemberCategoryVoteRepository;
import umc.catchy.domain.mapping.memberGroup.dao.MemberGroupRepository;
import umc.catchy.domain.mapping.memberPlaceVote.dao.MemberPlaceVoteRepository;
import umc.catchy.domain.place.dao.PlaceRepository;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.domain.placeReview.dao.PlaceReviewRepository;
import umc.catchy.domain.vote.dao.VoteRepository;
import umc.catchy.domain.vote.domain.Vote;
import umc.catchy.domain.vote.dto.response.CategoryVoteResultResponse;
import umc.catchy.domain.vote.dto.response.PlaceVoteListResponse;
import umc.catchy.support.fixture.CategoryVoteFixture;
import umc.catchy.support.fixture.GroupFixture;
import umc.catchy.support.fixture.PlaceFixture;
import umc.catchy.support.fixture.VoteFixture;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoteQueryServiceTest {

    @InjectMocks
    private VoteQueryService voteQueryService;

    @Mock
    private VoteRepository voteRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private CategoryVoteRepository categoryVoteRepository;
    @Mock
    private MemberCategoryVoteRepository memberCategoryVoteRepository;
    @Mock
    private MemberGroupRepository memberGroupRepository;
    @Mock
    private PlaceRepository placeRepository;
    @Mock
    private PlaceReviewRepository placeReviewRepository;
    @Mock
    private MemberPlaceVoteRepository memberPlaceVoteRepository;

    @Test
    @DisplayName("투표 결과 조회 성공 - 득표수 순 정렬 및 공동 순위 계산 확인")
    void getVoteResults_success() {
        Long voteId = 100L;
        Groups group = GroupFixture.createGroupWithId(1L, "그룹", "ABC");
        Vote vote = VoteFixture.createVoteWithId(voteId, group);
        CategoryVote cv1 = CategoryVoteFixture.createCategoryVote(501L, vote, BigCategory.CAFE);
        CategoryVote cv2 = CategoryVoteFixture.createCategoryVote(502L, vote, BigCategory.RESTAURANT);

        when(voteRepository.findById(voteId)).thenReturn(Optional.of(vote));
        when(categoryVoteRepository.findByVoteId(voteId)).thenReturn(List.of(cv1, cv2));

        // Arrays.asList를 사용하여 List<Object[]> 타입 추론 해결
        when(memberCategoryVoteRepository.countVotesByVoteIdGroupByCategory(voteId))
                .thenReturn(Arrays.asList(new Object[]{501L, 3L}, new Object[]{502L, 3L}));

        when(memberCategoryVoteRepository.findAllByVoteIdWithMember(voteId)).thenReturn(List.of());

        CategoryVoteResultResponse response = voteQueryService.getVoteResults(voteId);

        assertAll(
                () -> assertThat(response.results()).hasSize(2),
                () -> assertThat(response.results().get(0).rank()).isEqualTo(1),
                () -> assertThat(response.results().get(1).rank()).isEqualTo(1)
        );
    }

    @Test
    @DisplayName("카테고리별 장소 목록 조회 성공")
    void getPlacesByCategory_success() {
        Long groupId = 1L;
        Groups group = GroupFixture.createGroupWithId(groupId, "그룹", "ABC");
        Place place = PlaceFixture.createPlace(50L, 4.5);
        Slice<Place> slice = new SliceImpl<>(List.of(place));

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(placeRepository.getPlacesByCategoryWithPaging(any(), any(), any(), anyInt(), any(), any()))
                .thenReturn(slice);

        when(placeReviewRepository.countByPlaceIdsGroupByPlace(anyList()))
                .thenReturn(Arrays.<Object[]>asList(new Object[]{50L, 10L}));

        when(memberPlaceVoteRepository.findMembersByPlaceIdsAndGroupId(anyList(), eq(groupId)))
                .thenReturn(List.of());

        PlaceVoteListResponse response = voteQueryService.getPlacesByCategory(groupId, BigCategory.CAFE, 10, null);

        assertThat(response.places()).hasSize(1);
        assertThat(response.places().get(0).reviewCount()).isEqualTo(10L);
    }
}
