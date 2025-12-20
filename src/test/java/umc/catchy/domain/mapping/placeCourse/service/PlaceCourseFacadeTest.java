package umc.catchy.domain.mapping.placeCourse.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import umc.catchy.domain.mapping.placeCourse.dao.PlaceCourseRepository;
import umc.catchy.domain.mapping.placeCourse.dto.query.PlaceDto;
import umc.catchy.domain.mapping.placeCourse.dto.request.PlaceSearchRequest;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceDetailResponse;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlacePreviewResponse;
import umc.catchy.domain.mapping.placeLike.dao.PlaceLikeRepository;
import umc.catchy.domain.mapping.placeLike.domain.PlaceLike;
import umc.catchy.domain.mapping.placeLike.dto.response.LikedPlaceSliceResponse;
import umc.catchy.domain.mapping.placeVisit.dao.PlaceVisitRepository;
import umc.catchy.domain.mapping.placeVisit.domain.PlaceVisit;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.dao.PlaceRepository;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.domain.placeReview.dao.PlaceReviewRepository;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;
import umc.catchy.infra.google.GooglePlaceClient;
import umc.catchy.support.fixture.MemberFixture;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaceCourseFacadeTest {

    @InjectMocks
    private PlaceCourseFacade placeCourseFacade;

    @Mock
    private GooglePlaceClient googlePlaceClient;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PlaceVisitRepository placeVisitRepository;

    @Mock
    private PlaceLikeRepository placeLikeRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private PlaceReviewRepository placeReviewRepository;

    @Mock
    private PlaceCourseRepository placeCourseRepository;

    @Mock
    private PlaceCourseCommandService placeCourseCommandService;

    @Mock
    private Executor googlePlaceExecutor;

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = MemberFixture.createTestMember();

        lenient().doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(googlePlaceExecutor).execute(any());
    }

    @Test
    @DisplayName("프론트엔드 장소 검색 결과 처리 성공")
    void getPlacesByFrontend_success() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            PlaceSearchRequest req = new PlaceSearchRequest(100L, 37.5, 127.0, "장소", "주소");
            Place place = Place.builder().id(1L).poiId(100L).placeName("장소").rating(4.0).build();

            mocked.when(SecurityUtil::getCurrentMemberId).thenReturn(1L);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
            when(placeRepository.findAllByPoiIdIn(anyList())).thenReturn(List.of(place));
            when(placeReviewRepository.countReviewByPlaceIds(anyList())).thenReturn(Map.of(1L, 5L));
            when(placeLikeRepository.findLikedPlaceIdsByMemberAndPlaceIds(anyLong(), anyList())).thenReturn(Set.of(1L));

            List<PlacePreviewResponse> responses = placeCourseFacade.getPlacesByFrontend(List.of(req));

            assertAll(
                    () -> assertThat(responses).hasSize(1),
                    () -> assertThat(responses.get(0).reviewCount()).isEqualTo(5L),
                    () -> assertThat(responses.get(0).isLiked()).isTrue()
            );
        }
    }

    @Test
    @DisplayName("장소 상세 조회 성공")
    void getPlaceDetailByPlaceId_success() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            Long placeId = 1L;
            Place place = Place.builder().id(placeId).placeName("장소").rating(4.5).build();
            PlaceVisit mockVisit = mock(PlaceVisit.class);

            mocked.when(SecurityUtil::getCurrentMemberId).thenReturn(1L);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
            when(placeRepository.findByIdWithCategory(placeId)).thenReturn(Optional.of(place));
            when(placeReviewRepository.countByPlaceId(placeId)).thenReturn(10L);

            when(mockVisit.isVisited()).thenReturn(true);
            when(placeVisitRepository.findByPlaceAndMember(place, testMember)).thenReturn(Optional.of(mockVisit));
            when(placeLikeRepository.findByPlaceAndMember(place, testMember)).thenReturn(Optional.of(mock(PlaceLike.class)));

            PlaceDetailResponse response = placeCourseFacade.getPlaceDetailByPlaceId(placeId);

            assertAll(
                    () -> assertThat(response.placeId()).isEqualTo(placeId),
                    () -> assertThat(response.isVisited()).isTrue(),
                    () -> assertThat(response.liked()).isTrue(),
                    () -> assertThat(response.reviewCount()).isEqualTo(10L)
            );
        }
    }

    @Test
    @DisplayName("장소 상세 조회 실패 - 장소 없음")
    void getPlaceDetailByPlaceId_fail_placeNotFound() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            Long placeId = 1L;

            mocked.when(SecurityUtil::getCurrentMemberId).thenReturn(1L);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
            when(placeRepository.findByIdWithCategory(placeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> placeCourseFacade.getPlaceDetailByPlaceId(placeId))
                    .isInstanceOf(GeneralException.class)
                    .hasMessageContaining(ErrorStatus.PLACE_NOT_FOUND.getMessage());
        }
    }

    @Test
    @DisplayName("좋아요한 장소 목록 조회 성공")
    void searchLikedPlace_success() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            int pageSize = 10;
            Long lastPlaceId = 5L;
            PlaceDto placeDto = new PlaceDto(1L, "url", "이름", "카테고리", "주소", "시간", 4.0, 5L);
            Slice<PlaceDto> slice = new SliceImpl<>(List.of(placeDto));

            mocked.when(SecurityUtil::getCurrentMemberId).thenReturn(1L);
            when(placeCourseRepository.searchPlaceByLiked(1L, pageSize, lastPlaceId)).thenReturn(slice);

            LikedPlaceSliceResponse response = placeCourseFacade.searchLikedPlace(pageSize, lastPlaceId);

            assertAll(
                    () -> assertThat(response.content()).hasSize(1),
                    () -> assertThat(response.last()).isTrue(),
                    () -> assertThat(response.content().get(0).placeId()).isEqualTo(1L)
            );
        }
    }
}
