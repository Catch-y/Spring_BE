package umc.catchy.domain.vote.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
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
import umc.catchy.domain.mapping.memberPlaceVote.domain.MemberPlaceVote;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.dao.PlaceRepository;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.domain.placeReview.dao.PlaceReviewRepository;
import umc.catchy.domain.vote.dao.VoteRepository;
import umc.catchy.domain.vote.domain.Vote;
import umc.catchy.domain.vote.domain.VoteStatus;
import umc.catchy.domain.vote.dto.request.PlaceVoteRequest;
import umc.catchy.domain.vote.dto.request.VoteCreateRequest;
import umc.catchy.domain.vote.dto.response.CategoryVoteResultResponse;
import umc.catchy.domain.vote.dto.response.GroupVoteResultResponse;
import umc.catchy.domain.vote.dto.response.PlaceVoteListResponse;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;
import umc.catchy.infra.config.fcm.FCMService;
import umc.catchy.support.fixture.*;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoteServiceTest {

    @InjectMocks
    private VoteService voteService;

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private CategoryVoteRepository categoryVoteRepository;

    @Mock
    private MemberCategoryVoteRepository memberCategoryVoteRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberGroupRepository memberGroupRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private PlaceReviewRepository placeReviewRepository;

    @Mock
    private MemberPlaceVoteRepository memberPlaceVoteRepository;

    @Mock
    private FCMService fcmService;

    @Test
    @DisplayName("투표 생성 성공 - 모든 카테고리가 정확한 투표 객체와 매핑되어 저장되는지 확인한다.")
    void createVote_success() {
        // given
        Long groupId = 1L;
        VoteCreateRequest request = VoteFixture.createVoteRequest(groupId);
        Groups group = GroupFixture.createGroupWithId(groupId, "테스트 그룹", "ABC123");
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(memberGroupRepository.findMembersByGroupId(groupId)).thenReturn(List.of());

        Vote savedVote = VoteFixture.createVoteWithId(100L, group);
        when(voteRepository.save(any(Vote.class))).thenReturn(savedVote);

        ArgumentCaptor<CategoryVote> categoryVoteCaptor = ArgumentCaptor.forClass(CategoryVote.class);

        // when
        voteService.createVote(request);

        // then
        assertAll(
                () -> verify(categoryVoteRepository, times(BigCategory.values().length)).save(categoryVoteCaptor.capture()),
                () -> {
                    List<CategoryVote> capturedVotes = categoryVoteCaptor.getAllValues();
                    assertThat(capturedVotes).allMatch(cv -> cv.getVote().getId().equals(100L));

                    List<BigCategory> capturedCategories = capturedVotes.stream()
                            .map(CategoryVote::getBigCategory)
                            .toList();
                    assertThat(capturedCategories).containsExactlyInAnyOrder(BigCategory.values());
                }
        );
    }

    @Test
    @DisplayName("투표 생성 실패 - 존재하지 않는 그룹 ID인 경우 예외가 발생한다.")
    void createVote_fail_group_not_found() {
        // given
        Long invalidGroupId = 999L;
        VoteCreateRequest request = VoteFixture.createVoteRequest(invalidGroupId);

        when(groupRepository.findById(invalidGroupId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> voteService.createVote(request))
                .isInstanceOf(GeneralException.class)
                .hasMessageContaining(ErrorStatus.GROUP_NOT_FOUND.getMessage());

        verify(voteRepository, never()).save(any());
        verify(categoryVoteRepository, never()).save(any());
        verify(fcmService, never()).sendGroupMessageAsync(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("카테고리 투표 제출 성공 - 선택한 카테고리들에 대해 투표권을 저장한다.")
    void submitVote_success() {
        // given
        Long memberId = 1L;
        Long voteId = 100L;
        List<Long> categoryIds = List.of(501L, 502L);

        Member member = MemberFixture.createTestMember(memberId, "test@test.com");
        Vote vote = VoteFixture.createVoteWithId(voteId, mock(Groups.class));

        CategoryVote cv1 = CategoryVoteFixture.createCategoryVote(501L, vote, BigCategory.CAFE);
        CategoryVote cv2 = CategoryVoteFixture.createCategoryVote(502L, vote, BigCategory.RESTAURANT);

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentMemberId).thenReturn(memberId);

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

            // 이미 투표했는지 체크 - 아직 안 함
            when(memberCategoryVoteRepository.existsByVoteIdAndMemberId(voteId, memberId)).thenReturn(false);

            when(categoryVoteRepository.findById(501L)).thenReturn(Optional.of(cv1));
            when(categoryVoteRepository.findById(502L)).thenReturn(Optional.of(cv2));

            // 투표 완료 체크를 위한 투표 객체 조회 모킹
            when(voteRepository.findById(voteId)).thenReturn(Optional.of(vote));
            when(memberGroupRepository.countByGroupId(any())).thenReturn(5);
            when(memberCategoryVoteRepository.countDistinctMembersByVoteId(voteId)).thenReturn(3);

            // when
            voteService.submitVote(voteId, categoryIds);

            // then
            assertAll(
                    () -> verify(memberCategoryVoteRepository, times(categoryIds.size())).save(any()),
                    () -> verify(voteRepository, atLeastOnce()).findById(voteId)
            );
        }
    }

    @Test
    @DisplayName("카테고리 투표 제출 실패 - 이미 투표한 유저는 다시 투표할 수 없다.")
    void submitVote_fail_already_voted() {
        // given
        Long memberId = 1L;
        Long voteId = 100L;
        List<Long> categoryIds = List.of(501L, 502L);

        Member member = MemberFixture.createTestMember(memberId, "test@test.com");

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentMemberId).thenReturn(memberId);

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

            // 이미 투표한 것으로 설정
            when(memberCategoryVoteRepository.existsByVoteIdAndMemberId(voteId, memberId)).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> voteService.submitVote(voteId, categoryIds))
                    .isInstanceOf(GeneralException.class)
                    .hasMessageContaining(ErrorStatus.CATEGORY_ALREADY_VOTED.getMessage());

            verify(memberCategoryVoteRepository, never()).save(any());
            verify(voteRepository, never()).findById(anyLong());
        }
    }

    @Test
    @DisplayName("투표 상태 변경 성공 - 모든 그룹원이 투표를 마치면 상태가 COMPLETED로 변경된다.")
    void checkAndUpdateVoteCompletion_success() {
        // given
        Long voteId = 100L;
        Long groupId = 1L;

        Groups group = GroupFixture.createGroupWithId(groupId, "테스트 그룹", "INVITE123");
        Vote vote = VoteFixture.createVoteWithId(voteId, group);

        when(voteRepository.findById(voteId)).thenReturn(Optional.of(vote));

        // 전체 인원 5명, 투표 완료 인원 5명으로 설정
        when(memberGroupRepository.countByGroupId(groupId)).thenReturn(5);
        when(memberCategoryVoteRepository.countDistinctMembersByVoteId(voteId)).thenReturn(5);

        // when
        voteService.checkAndUpdateVoteCompletion(voteId);

        // then
        assertAll(
                () -> assertThat(vote.getStatus()).isEqualTo(VoteStatus.COMPLETED),
                () -> verify(voteRepository, times(1)).save(vote)
        );
    }

    @Test
    @DisplayName("투표 상태 유지 - 투표 인원이 전체 인원보다 적으면 상태가 IN_PROGRESS로 유지된다.")
    void checkAndUpdateVoteCompletion_keep_in_progress() {
        // given
        Long voteId = 100L;
        Long groupId = 1L;

        Groups group = GroupFixture.createGroupWithId(groupId, "테스트 그룹", "INVITE123");
        Vote vote = VoteFixture.createVoteWithId(voteId, group);

        when(voteRepository.findById(voteId)).thenReturn(Optional.of(vote));

        // 전체 인원 5명, 투표 완료 인원 4명 (아직 1명 남음)
        when(memberGroupRepository.countByGroupId(groupId)).thenReturn(5);
        when(memberCategoryVoteRepository.countDistinctMembersByVoteId(voteId)).thenReturn(4);

        // when
        voteService.checkAndUpdateVoteCompletion(voteId);

        // then
        assertAll(
                () -> assertThat(vote.getStatus()).isEqualTo(VoteStatus.IN_PROGRESS),
                () -> verify(voteRepository, never()).save(any(Vote.class))
        );
    }

    @Test
    @DisplayName("투표 결과 조회 성공 - 득표수 순으로 정렬되고 공동 순위가 정확히 계산되어야 한다.")
    void getVoteResults_success_with_ranking() {
        // given
        Long voteId = 100L;
        Long groupId = 1L;
        Groups group = GroupFixture.createGroupWithId(groupId, "테스트 그룹", "ABC123");
        Vote vote = VoteFixture.createVoteWithId(voteId, group);

        CategoryVote cv1 = CategoryVoteFixture.createCategoryVote(501L, vote, BigCategory.CAFE);
        CategoryVote cv2 = CategoryVoteFixture.createCategoryVote(502L, vote, BigCategory.RESTAURANT);
        CategoryVote cv3 = CategoryVoteFixture.createCategoryVote(503L, vote, BigCategory.SPORT);

        when(voteRepository.findById(voteId)).thenReturn(Optional.of(vote));
        when(memberGroupRepository.countByGroupId(groupId)).thenReturn(5);
        when(categoryVoteRepository.findByVoteId(voteId)).thenReturn(List.of(cv1, cv2, cv3));

        // 각 카테고리별 득표 설정 (cv1: 3표, cv2: 3표, cv3: 1표 -> 공동 1위 2개, 3위 1개 기대)
        when(memberCategoryVoteRepository.countByVoteIdAndCategoryVoteId(voteId, 501L)).thenReturn(3);
        when(memberCategoryVoteRepository.countByVoteIdAndCategoryVoteId(voteId, 502L)).thenReturn(3);
        when(memberCategoryVoteRepository.countByVoteIdAndCategoryVoteId(voteId, 503L)).thenReturn(1);

        // 멤버 리스트는 빈 리스트로 간단히 모킹
        when(memberCategoryVoteRepository.findMembersByCategoryVoteId(anyLong())).thenReturn(List.of());

        // when
        CategoryVoteResultResponse response = voteService.getVoteResults(voteId);

        // then
        assertAll(
                () -> assertThat(response.totalMembers()).isEqualTo(5),
                () -> assertThat(response.results()).hasSize(3),

                // 득표수 높은 순 정렬 확인
                () -> assertThat(response.results().get(0).voteCount()).isEqualTo(3),
                () -> assertThat(response.results().get(1).voteCount()).isEqualTo(3),
                () -> assertThat(response.results().get(2).voteCount()).isEqualTo(1),

                // 순위 로직 검증 (공동 1위 처리)
                () -> assertThat(response.results().get(0).rank()).isEqualTo(1),
                () -> assertThat(response.results().get(1).rank()).isEqualTo(1),
                () -> assertThat(response.results().get(2).rank()).isEqualTo(3)
        );
    }

    @Test
    @DisplayName("투표 최종 결과 조회 성공 - 과반수 이상 득표한 카테고리만 장소 개수와 함께 반환된다.")
    void getGroupVoteResults_success_with_threshold() {
        // given
        Long groupId = 1L;
        Long voteId = 100L;
        String location = "서울시 강남구 테헤란로";

        Groups group = GroupFixture.createGroupWithId(groupId, "테스트 그룹", "ABC123");
        Vote vote = VoteFixture.createVoteWithId(voteId, group);

        when(voteRepository.findByIdAndGroupId(voteId, groupId)).thenReturn(Optional.of(vote));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(memberGroupRepository.findMembersByGroupId(groupId)).thenReturn(List.of());

        when(memberGroupRepository.countByGroupId(groupId)).thenReturn(5);

        CategoryVote cv1 = CategoryVoteFixture.createCategoryVote(501L, vote, BigCategory.CAFE);
        CategoryVote cv2 = CategoryVoteFixture.createCategoryVote(502L, vote, BigCategory.RESTAURANT);
        when(categoryVoteRepository.findByVoteId(voteId)).thenReturn(List.of(cv1, cv2));

        // cv1: 3표 (과반수 통과), cv2: 2표 (과반수 미달)
        when(memberCategoryVoteRepository.countByVoteIdAndCategoryVoteId(voteId, 501L)).thenReturn(3);
        when(memberCategoryVoteRepository.countByVoteIdAndCategoryVoteId(voteId, 502L)).thenReturn(2);

        // 과반수를 넘긴 cv1에 대해서만 장소 조회가 일어남 (장소 10개 있다고 가정)
        when(placeRepository.findByBigCategoryAndLocation(eq(BigCategory.CAFE), anyString(), anyString()))
                .thenReturn(List.of(new Place(), new Place()));

        // when
        GroupVoteResultResponse response = voteService.getGroupVoteResults(groupId, voteId);

        // then
        assertAll(
                () -> assertThat(response.groupLocation()).isEqualTo(location),
                () -> assertThat(response.categories()).hasSize(1),
                () -> assertThat(response.categories().get(0).category()).isEqualTo("CAFE"),
                () -> assertThat(response.categories().get(0).count()).isEqualTo(2),
                () -> verify(fcmService, times(1)).sendGroupMessageAsync(any(), anyString(), anyString())
        );
    }

    @Test
    @DisplayName("장소 투표 추가 성공 - 기존 투표가 없으면 새로운 투표를 생성한다.")
    void togglePlaceVote_add_success() {
        // given
        Long memberId = 1L;
        Long placeId = 50L;
        Long voteId = 100L;
        Long groupId = 10L;
        PlaceVoteRequest request = new PlaceVoteRequest(placeId);

        Member member = MemberFixture.createTestMember(memberId, "test@test.com");
        Groups group = GroupFixture.createGroupWithId(groupId, "테스트 그룹", "ABC1234");
        Vote vote = VoteFixture.createVoteWithId(voteId, group);
        Place place = PlaceFixture.createPlace(placeId, 4.5);

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentMemberId).thenReturn(memberId);

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
            when(placeRepository.findById(placeId)).thenReturn(Optional.of(place));
            when(voteRepository.findById(voteId)).thenReturn(Optional.of(vote));
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

            when(memberPlaceVoteRepository.findByMemberIdAndPlaceIdAndVoteId(memberId, placeId, voteId))
                    .thenReturn(null);

            // when
            String message = voteService.togglePlaceVote(voteId, groupId, request);

            // then
            assertAll(
                    () -> assertThat(message).isEqualTo("Vote added successfully."),
                    () -> verify(memberPlaceVoteRepository, times(1)).save(any(MemberPlaceVote.class)),
                    () -> verify(memberPlaceVoteRepository, never()).delete(any())
            );
        }
    }

    @Test
    @DisplayName("장소 투표 취소 성공 - 이미 투표한 장소라면 기존 투표를 삭제한다.")
    void togglePlaceVote_remove_success() {
        // given
        Long memberId = 1L;
        Long placeId = 50L;
        Long voteId = 100L;
        Long groupId = 10L;
        PlaceVoteRequest request = new PlaceVoteRequest(placeId);

        Member member = MemberFixture.createTestMember(memberId, "test@test.com");
        Groups group = GroupFixture.createGroupWithId(groupId, "테스트 그룹", "ABC1234");
        Vote vote = VoteFixture.createVoteWithId(voteId, group);
        Place place = PlaceFixture.createPlace(placeId, 4.5);

        MemberPlaceVote existingVote = MemberPlaceVoteFixture.create(place, member, vote, group);

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentMemberId).thenReturn(memberId);

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
            when(placeRepository.findById(placeId)).thenReturn(Optional.of(place));
            when(voteRepository.findById(voteId)).thenReturn(Optional.of(vote));
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

            when(memberPlaceVoteRepository.findByMemberIdAndPlaceIdAndVoteId(memberId, placeId, voteId))
                    .thenReturn(existingVote);

            // when
            String message = voteService.togglePlaceVote(voteId, groupId, request);

            // then
            assertAll(
                    () -> assertThat(message).isEqualTo("Vote removed successfully."),
                    () -> verify(memberPlaceVoteRepository, times(1)).delete(existingVote),
                    () -> verify(memberPlaceVoteRepository, never()).save(any())
            );
        }
    }

    @Test
    @DisplayName("카테고리별 장소 목록 조회 성공 - 페이징된 장소 리스트와 각 장소에 투표한 멤버 정보를 반환한다.")
    void getPlacesByCategory_success() {
        // given
        Long groupId = 1L;
        String category = "CAFE";
        int pageSize = 10;
        Long lastPlaceId = null;

        Groups group = GroupFixture.createGroupWithId(groupId, "테스트 그룹", "ABC1234");
        Place place1 = PlaceFixture.createPlace(50L, 4.5);
        Place place2 = PlaceFixture.createPlace(51L, 4.0);
        Member voter = MemberFixture.createTestMember(2L, "voter@test.com");

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

        // Slice 객체 생성 (place1, place2가 담겨 있고 다음 페이지가 없다고 가정)
        Slice<Place> placeSlice = new SliceImpl<>(List.of(place1, place2), PageRequest.of(0, pageSize), false);

        when(placeRepository.getPlacesByCategoryWithPaging(
                eq(BigCategory.CAFE), anyString(), anyString(), eq(pageSize), eq(lastPlaceId), eq(groupId)))
                .thenReturn(placeSlice);

        // 각 장소별 리뷰 개수 모킹
        when(placeReviewRepository.countByPlaceId(50L)).thenReturn(10L);
        when(placeReviewRepository.countByPlaceId(51L)).thenReturn(5L);

        // 각 장소별 투표 멤버 모킹 (50번 장소엔 voter가 투표, 51번은 아무도 안 함)
        when(memberPlaceVoteRepository.findMembersByPlaceIdAndGroupId(50L, groupId)).thenReturn(List.of(voter));
        when(memberPlaceVoteRepository.findMembersByPlaceIdAndGroupId(51L, groupId)).thenReturn(List.of());

        // when
        PlaceVoteListResponse response = voteService.getPlacesByCategory(groupId, category, pageSize, lastPlaceId);

        // then
        assertAll(
                () -> assertThat(response.groupLocation()).isEqualTo(group.getGroupLocation()),
                () -> assertThat(response.isLast()).isTrue(),
                () -> assertThat(response.places()).hasSize(2),

                // 첫 번째 장소 검증 (리뷰 수 및 투표 멤버)
                () -> assertThat(response.places().get(0).placeId()).isEqualTo(50L),
                () -> assertThat(response.places().get(0).reviewCount()).isEqualTo(10L),
                () -> assertThat(response.places().get(0).votedMembers()).hasSize(1),
                () -> assertThat(response.places().get(0).votedMembers().get(0).nickname()).isEqualTo(voter.getNickname()),

                // 두 번째 장소 검증
                () -> assertThat(response.places().get(1).placeId()).isEqualTo(51L),
                () -> assertThat(response.places().get(1).votedMembers()).isEmpty()
        );
    }
}
