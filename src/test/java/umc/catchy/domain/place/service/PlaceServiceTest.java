package umc.catchy.domain.place.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import umc.catchy.domain.category.dao.CategoryRepository;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.category.domain.Category;
import umc.catchy.domain.course.dao.CourseRepository;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.mapping.placeCourse.dto.query.PlacePreviewDto;
import umc.catchy.domain.mapping.placeCourse.dto.query.PlaceSearchDto;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlacePreviewResponse;
import umc.catchy.domain.mapping.placeLike.dao.PlaceLikeRepository;
import umc.catchy.domain.mapping.placeLike.domain.PlaceLike;
import umc.catchy.domain.mapping.placeLike.dto.response.PlaceLikedResponse;
import umc.catchy.domain.mapping.placeVisit.dao.PlaceVisitRepository;
import umc.catchy.domain.mapping.placeVisit.domain.PlaceVisit;
import umc.catchy.domain.mapping.placeVisit.dto.response.PlaceVisitedDateResponse;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.dao.PlaceRepository;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.domain.place.dto.request.SetCategoryRequest;
import umc.catchy.domain.place.dto.response.PlaceSearchSliceResponse;
import umc.catchy.domain.place.dto.response.RecommendationContext;
import umc.catchy.global.common.dto.SliceResponse;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;
import umc.catchy.support.fixture.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    @InjectMocks
    private PlaceService placeService;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PlaceVisitRepository placeVisitRepository;

    @Mock
    private PlaceLikeRepository placeLikeRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private PlaceRecommendationService recommendationService;

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = MemberFixture.createTestMember();
    }

    @Test
    @DisplayName("장소 카테고리 설정 성공")
    void setCategories_success() {
        Long placeId = 1L;
        SetCategoryRequest request = new SetCategoryRequest("카페", "프랜차이즈");
        Place place = PlaceFixture.createPlaceWithoutCategory(placeId);
        Category category = CategoryFixture.createCategory(1L, "프랜차이즈", BigCategory.CAFE);

        when(placeRepository.findById(placeId)).thenReturn(Optional.of(place));
        when(categoryRepository.findByBigCategoryAndName(BigCategory.CAFE, "프랜차이즈"))
                .thenReturn(Optional.of(category));

        placeService.setCategories(placeId, request);

        assertAll(
                () -> assertThat(place.getCategory()).isEqualTo(category),
                () -> assertThat(place.getCategory().getBigCategory()).isEqualTo(BigCategory.CAFE)
        );
    }

    @Test
    @DisplayName("장소 카테고리 설정 실패 - 유효하지 않은 카테고리")
    void setCategories_fail_invalidCategory() {
        Long placeId = 1L;
        SetCategoryRequest request = new SetCategoryRequest("카페", "잘못된카테고리");
        Place place = PlaceFixture.createPlaceWithoutCategory(placeId);

        when(placeRepository.findById(placeId)).thenReturn(Optional.of(place));
        when(categoryRepository.findByBigCategoryAndName(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> placeService.setCategories(placeId, request))
                .isInstanceOf(GeneralException.class)
                .hasMessageContaining(ErrorStatus.INVALID_CATEGORY.getMessage());
    }

    @Test
    @DisplayName("장소 추천 조회 성공")
    void recommendPlaces_success() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            Double lat = 37.5665;
            Double lon = 126.9780;
            mocked.when(SecurityUtil::getCurrentMemberId).thenReturn(1L);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));

            List<PlaceVisit> visits = Collections.emptyList();
            when(placeVisitRepository.findAllByMemberWithPlaceAndCategory(testMember)).thenReturn(visits);

            RecommendationContext context = new RecommendationContext(List.of(1L), Map.of(1L, 14));
            when(recommendationService.analyzeVisitHistory(visits)).thenReturn(context);

            PlacePreviewDto dto = mock(PlacePreviewDto.class);
            Slice<PlacePreviewDto> slice = new SliceImpl<>(List.of(dto), PageRequest.of(0, 10), false);
            when(placeRepository.recommendPlacesByActivityData(anyLong(), anyDouble(), anyDouble(), anyList(), anyMap(), anyInt(), anyInt()))
                    .thenReturn(slice);

            SliceResponse<PlacePreviewResponse> response = placeService.recommendPlaces(lat, lon, 10, 0);

            assertAll(
                    () -> assertThat(response.content()).hasSize(1),
                    () -> assertThat(response.isLast()).isTrue()
            );
        }
    }

    @Test
    @DisplayName("장소 검색 성공")
    void searchPlaceByCategoryOrName_success() {
        String keyword = "테스트";
        PlaceSearchDto dto = mock(PlaceSearchDto.class);
        Slice<PlaceSearchDto> slice = new SliceImpl<>(List.of(dto), PageRequest.of(0, 10), false);

        when(placeRepository.searchPlace(anyInt(), anyString(), any(), any()))
                .thenReturn(slice);

        PlaceSearchSliceResponse response = placeService.searchPlaceByCategoryOrName(10, keyword, null, null);

        assertAll(
                () -> assertThat(response.content()).hasSize(1),
                () -> assertThat(response.last()).isTrue()
        );
    }

    @Test
    @DisplayName("장소 좋아요 토글 성공 - 신규 좋아요")
    void togglePlaceLike_success_new() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            Long placeId = 1L;
            Place place = PlaceFixture.createPlace(placeId, 4.5);
            mocked.when(SecurityUtil::getCurrentMemberId).thenReturn(1L);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
            when(placeRepository.findById(placeId)).thenReturn(Optional.of(place));
            when(placeLikeRepository.findByPlaceAndMember(place, testMember)).thenReturn(Optional.empty());

            PlaceLikedResponse response = placeService.togglePlaceLike(placeId);

            assertAll(
                    () -> assertThat(response.isLiked()).isTrue(),
                    () -> verify(placeLikeRepository).save(any(PlaceLike.class))
            );
        }
    }

    @Test
    @DisplayName("장소 좋아요 토글 성공 - 기존 좋아요 취소")
    void togglePlaceLike_success_toggleExisting() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            Long placeId = 1L;
            Place place = PlaceFixture.createPlace(placeId, 4.5);
            PlaceLike existingLike = PlaceLikeFixture.createLikedPlace(testMember, place);

            mocked.when(SecurityUtil::getCurrentMemberId).thenReturn(1L);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
            when(placeRepository.findById(placeId)).thenReturn(Optional.of(place));
            when(placeLikeRepository.findByPlaceAndMember(place, testMember)).thenReturn(Optional.of(existingLike));

            PlaceLikedResponse response = placeService.togglePlaceLike(placeId);

            assertThat(response.isLiked()).isFalse();
        }
    }

    @Test
    @DisplayName("장소 방문 날짜 조회 성공")
    void getPlaceVisitDate_success() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            Long courseId = 1L;
            Long placeId = 10L;
            Place place = PlaceFixture.createPlace(placeId, 4.0);
            Course course = CourseFixture.createTestCourse(testMember);
            LocalDate visit1 = LocalDate.of(2025, 12, 16);
            LocalDate visit2 = LocalDate.of(2025, 12, 17);

            PlaceVisit pv1 = PlaceVisitFixture.createPlaceVisit(1L, testMember, place, course, visit1);
            PlaceVisit pv2 = PlaceVisitFixture.createPlaceVisit(2L, testMember, place, course, visit2);

            mocked.when(SecurityUtil::getCurrentMemberId).thenReturn(1L);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
            when(placeRepository.findById(placeId)).thenReturn(Optional.of(place));
            when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
            when(placeVisitRepository.findAllByMemberAndPlaceAndCourseAndIsVisitedTrue(testMember, place, course))
                    .thenReturn(List.of(pv1, pv2));

            PlaceVisitedDateResponse response = placeService.getPlaceVisitDate(courseId, placeId);

            assertAll(
                    () -> assertThat(response.visitedDate()).hasSize(2),
                    () -> assertThat(response.visitedDate()).containsExactly(visit1, visit2)
            );
        }
    }

    @Test
    @DisplayName("장소 방문 날짜 조회 실패 - 코스 없음")
    void getPlaceVisitDate_fail_courseNotFound() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            Long courseId = 1L;
            Long placeId = 1L;
            Place place = PlaceFixture.createPlace(placeId, 4.5);

            mocked.when(SecurityUtil::getCurrentMemberId).thenReturn(1L);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
            when(placeRepository.findById(placeId)).thenReturn(Optional.of(place));
            when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> placeService.getPlaceVisitDate(courseId, placeId))
                    .isInstanceOf(GeneralException.class)
                    .hasMessageContaining(ErrorStatus.COURSE_NOT_FOUND.getMessage());
        }
    }

}
