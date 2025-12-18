package umc.catchy.domain.course.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.course.dao.CourseRepository;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.course.domain.CourseType;
import umc.catchy.domain.course.dto.response.CourseDetailResponse;
import umc.catchy.domain.course.dto.response.GptCourseInfoResponse;
import umc.catchy.domain.course.dto.response.GptPlaceInfoResponse;
import umc.catchy.domain.courseReview.dao.CourseReviewRepository;
import umc.catchy.domain.mapping.memberCourse.dao.MemberCourseRepository;
import umc.catchy.domain.mapping.memberCourse.domain.MemberCourse;
import umc.catchy.domain.mapping.memberCourse.dto.response.MemberCourseResponse;
import umc.catchy.domain.mapping.memberCourse.dto.response.MemberCourseSliceResponse;
import umc.catchy.domain.mapping.placeCourse.dao.PlaceCourseRepository;
import umc.catchy.domain.mapping.placeCourse.domain.PlaceCourse;
import umc.catchy.domain.mapping.placeVisit.dao.PlaceVisitRepository;
import umc.catchy.domain.mapping.placeVisit.domain.PlaceVisit;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.dao.PlaceRepository;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;
import umc.catchy.support.fixture.CourseFixture;
import umc.catchy.support.fixture.MemberFixture;
import umc.catchy.support.fixture.PlaceFixture;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @InjectMocks
    private CourseService courseService;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseReviewRepository courseReviewRepository;

    @Mock
    private PlaceCourseRepository placeCourseRepository;

    @Mock
    private PlaceVisitRepository placeVisitRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberCourseRepository memberCourseRepository;

    @Mock
    private PlaceRepository placeRepository;

    private Member testMember;
    private Course testCourse;

    @BeforeEach
    void setUp() {
        testMember = MemberFixture.createTestMember();
        testCourse = CourseFixture.createTestCourse(testMember);
    }

    private void mockSecurityUtil(MockedStatic<SecurityUtil> mockedSecurityUtil) {
        mockedSecurityUtil.when(SecurityUtil::getCurrentMemberId).thenReturn(1L);
    }

    @Test
    @DisplayName("코스 상세 조회 성공 - 방문 여부, 북마크, 리뷰 수 포함")
    void getCourseDetails_success() {
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            // given
            Long courseId = 1L;
            mockSecurityUtil(mockedSecurityUtil);

            Course courseWithReview = CourseFixture.createTestCourseWithReviewStatus(testMember, true);

            // 1. 기본 코스 및 회원 조회
            when(courseRepository.findById(courseId)).thenReturn(Optional.of(courseWithReview));
            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));

            // 2. 장소 생성
            Place place1 = PlaceFixture.createPlace(10L, 5.0);

            // PlaceCourse 연결
            PlaceCourse placeCourse1 = PlaceCourse.builder()
                    .course(courseWithReview)
                    .place(place1)
                    .placeOrder(1)
                    .build();
            when(placeCourseRepository.findAllByCourseWithPlace(courseWithReview)).thenReturn(List.of(placeCourse1));

            // 3. 방문 여부 설정 (isVisited = true)
            PlaceVisit placeVisit = PlaceVisit.builder()
                    .member(testMember)
                    .place(place1)
                    .isVisited(true)
                    .build();
            when(placeVisitRepository.findAllByPlaceIdsAndMember(List.of(10L), testMember))
                    .thenReturn(List.of(placeVisit));

            // 4. 리뷰 수 및 북마크 설정
            when(courseReviewRepository.countAllByCourse(courseWithReview)).thenReturn(5);

            MemberCourse memberCourse = MemberCourse.builder()
                    .member(testMember)
                    .course(courseWithReview)
                    .bookmark(true)
                    .build();
            when(memberCourseRepository.findByCourseAndMember(courseWithReview, testMember))
                    .thenReturn(Optional.of(memberCourse));

            // when
            CourseDetailResponse response = courseService.getCourseDetails(courseId);

            // then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.courseName()).isEqualTo(courseWithReview.getCourseName()),
                    () -> assertThat(response.reviewCount()).isEqualTo(5),
                    () -> assertThat(response.isBookMarked()).isTrue(),
                    () -> assertThat(response.placeInfos()).hasSize(1),
                    () -> assertThat(response.placeInfos().get(0).placeId()).isEqualTo(10L),
                    () -> assertThat(response.placeInfos().get(0).category()).isEqualTo(BigCategory.CAFE),
                    () -> assertThat(response.placeInfos().get(0).isVisited()).isTrue()
            );

            verify(courseRepository).findById(courseId);
            verify(placeVisitRepository).findAllByPlaceIdsAndMember(anyList(), eq(testMember));
        }
    }

    @Test
    @DisplayName("AI 코스 저장 및 장소 등록 성공 - 시간 파싱, 평점 계산, 순서 보장")
    void saveCourseAndPlaces_success() {
        // given
        GptPlaceInfoResponse placeInfo1 = new GptPlaceInfoResponse(
                10L, "장소1", "img.jpg", "CAFE", "주소1", "09:00", 5.0, 10
        );
        GptPlaceInfoResponse placeInfo2 = new GptPlaceInfoResponse(
                20L, "장소2", "img.jpg", "REST", "주소2", "10:00", 3.0, 5
        );

        GptCourseInfoResponse gptResponse = new GptCourseInfoResponse(
                "AI 추천 코스", "AI 설명", "09:00 ~ 24:00",
                List.of(placeInfo1, placeInfo2)
        );

        Place place1 = PlaceFixture.createPlace(10L, 5.0);
        Place place2 = PlaceFixture.createPlace(20L, 3.0);

        when(placeRepository.findAllById(anyList())).thenReturn(List.of(place2, place1));

        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course c = invocation.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 100L);
            return c;
        });

        // when
        courseService.saveCourseAndPlaces(gptResponse, testMember);

        // then
        ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(courseCaptor.capture());

        Course savedCourse = courseCaptor.getValue();

        assertAll(
                () -> assertThat(savedCourse.getCourseName()).isEqualTo("AI 추천 코스"),
                () -> assertThat(savedCourse.getRecommendTimeEnd()).isEqualTo(LocalTime.MIDNIGHT),
                () -> assertThat(savedCourse.getRating()).isEqualTo(4.0)
        );

        // 2. 장소 순서 검증
        ArgumentCaptor<List<PlaceCourse>> placeCourseCaptor = ArgumentCaptor.forClass(List.class);
        verify(placeCourseRepository).saveAll(placeCourseCaptor.capture());

        List<PlaceCourse> savedPlaceCourses = placeCourseCaptor.getValue();
        assertAll(
                () -> assertThat(savedPlaceCourses).hasSize(2),
                () -> assertThat(savedPlaceCourses.get(0).getPlaceOrder()).isEqualTo(1),
                () -> assertThat(savedPlaceCourses.get(1).getPlaceOrder()).isEqualTo(2)
        );
    }

    @Test
    @DisplayName("AI 코스 저장 실패 - 잘못된 시간 포맷")
    void saveCourseAndPlaces_fail_invalid_time() {
        // given
        String invalidTime = "25:00 ~ 18:00";

        GptCourseInfoResponse gptResponse = new GptCourseInfoResponse(
                "AI 코스", "설명", invalidTime, List.of()
        );

        // when & then
        assertThatThrownBy(() -> courseService.saveCourseAndPlaces(gptResponse, testMember))
                .isInstanceOf(GeneralException.class)
                .hasMessageContaining(ErrorStatus.INVALID_REQUEST_INFO.getMessage());

        verify(courseRepository, never()).save(any());
        verify(placeCourseRepository, never()).saveAll(any());
    }
}
