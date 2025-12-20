package umc.catchy.domain.mapping.placeVisit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.catchy.domain.course.dao.CourseRepository;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.mapping.memberCourse.dao.MemberCourseRepository;
import umc.catchy.domain.mapping.memberCourse.domain.MemberCourse;
import umc.catchy.domain.mapping.placeCourse.dao.PlaceCourseRepository;
import umc.catchy.domain.mapping.placeCourse.domain.PlaceCourse;
import umc.catchy.domain.mapping.placeVisit.dao.PlaceVisitRepository;
import umc.catchy.domain.mapping.placeVisit.domain.PlaceVisit;
import umc.catchy.domain.mapping.placeVisit.dto.response.PlaceVisitedResponse;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.dao.PlaceRepository;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;
import umc.catchy.support.fixture.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaceVisitServiceTest {

    @InjectMocks
    private PlaceVisitService placeVisitService;

    @Mock
    private PlaceVisitRepository placeVisitRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private MemberCourseRepository memberCourseRepository;

    @Mock
    private PlaceCourseRepository placeCourseRepository;

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = MemberFixture.createTestMember();
    }

    @Test
    @DisplayName("방문 체크 성공 - 과반수 달성 시 코스 완료 처리")
    void check_success_completesCourse() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            Long courseId = 1L;
            Long placeId = 10L;
            Place place1 = PlaceFixture.createPlace(placeId, 4.5);
            Place place2 = PlaceFixture.createPlace(11L, 4.0);
            Place place3 = PlaceFixture.createPlace(12L, 3.5);

            Course course = CourseFixture.createTestCourse(testMember);
            MemberCourse memberCourse = MemberCourseFixture.createTestMemberCourse(testMember, course);

            List<PlaceCourse> placeCourses = List.of(
                    PlaceCourseFixture.createPlaceCourse(1L, course, place1, 1),
                    PlaceCourseFixture.createPlaceCourse(2L, course, place2, 2),
                    PlaceCourseFixture.createPlaceCourse(3L, course, place3, 3)
            );

            PlaceVisit currentVisit = PlaceVisit.builder().id(100L).isVisited(true).place(place1).build();
            List<PlaceVisit> visits = List.of(
                    currentVisit,
                    PlaceVisit.builder().place(place2).build()
            );

            mocked.when(SecurityUtil::getCurrentMemberId).thenReturn(1L);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
            when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
            when(placeRepository.findById(placeId)).thenReturn(Optional.of(place1));
            when(memberCourseRepository.findByCourseAndMember(course, testMember)).thenReturn(Optional.of(memberCourse));
            when(placeVisitRepository.findByPlaceAndMemberAndCourseAndVisitedDate(any(), any(), any(), any()))
                    .thenReturn(Optional.empty());
            when(placeVisitRepository.save(any())).thenReturn(currentVisit);
            when(placeCourseRepository.findAllByCourseWithPlace(course)).thenReturn(placeCourses);
            when(placeVisitRepository.findAllByCourseAndMemberWithPlace(course, testMember)).thenReturn(visits);

            PlaceVisitedResponse response = placeVisitService.check(courseId, placeId);

            assertAll(
                    () -> assertThat(response.placeVisitId()).isEqualTo(100L),
                    () -> assertThat(response.isVisited()).isTrue(),
                    () -> assertThat(course.getParticipantsNumber()).isEqualTo(1L),
                    () -> assertThat(memberCourse.isVisited()).isTrue(),
                    () -> assertThat(memberCourse.getVisitedDate()).isEqualTo(LocalDate.now())
            );
        }
    }

    @Test
    @DisplayName("방문 체크 실패 - 오늘 이미 방문함")
    void check_fail_alreadyVisitedToday() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            Long courseId = 1L;
            Long placeId = 10L;
            Place place = PlaceFixture.createPlace(placeId, 4.5);
            Course course = CourseFixture.createTestCourse(testMember);
            MemberCourse memberCourse = MemberCourseFixture.createTestMemberCourse(testMember, course);

            mocked.when(SecurityUtil::getCurrentMemberId).thenReturn(1L);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
            when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
            when(placeRepository.findById(placeId)).thenReturn(Optional.of(place));
            when(memberCourseRepository.findByCourseAndMember(course, testMember)).thenReturn(Optional.of(memberCourse));
            when(placeVisitRepository.findByPlaceAndMemberAndCourseAndVisitedDate(eq(place), eq(testMember), eq(course), any()))
                    .thenReturn(Optional.of(mock(PlaceVisit.class)));

            assertThatThrownBy(() -> placeVisitService.check(courseId, placeId))
                    .isInstanceOf(GeneralException.class)
                    .hasMessageContaining(ErrorStatus.PLACE_VISIT_ALREADY_CHECK.getMessage());
        }
    }

    @Test
    @DisplayName("방문 체크 실패 - 코스 멤버 아님")
    void check_fail_notCourseMember() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            Long courseId = 1L;
            Long placeId = 10L;
            Place place = PlaceFixture.createPlace(placeId, 4.5);
            Course course = CourseFixture.createTestCourse(testMember);

            mocked.when(SecurityUtil::getCurrentMemberId).thenReturn(1L);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
            when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
            when(placeRepository.findById(placeId)).thenReturn(Optional.of(place));
            when(memberCourseRepository.findByCourseAndMember(course, testMember)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> placeVisitService.check(courseId, placeId))
                    .isInstanceOf(GeneralException.class)
                    .hasMessageContaining(ErrorStatus.COURSE_INVALID_MEMBER.getMessage());
        }
    }

    @Test
    @DisplayName("방문 체크 실패 - 장소 정보 없음")
    void check_fail_placeNotFound() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            Long courseId = 1L;
            Long placeId = 10L;

            mocked.when(SecurityUtil::getCurrentMemberId).thenReturn(1L);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
            when(courseRepository.findById(courseId)).thenReturn(Optional.of(mock(Course.class)));
            when(placeRepository.findById(placeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> placeVisitService.check(courseId, placeId))
                    .isInstanceOf(GeneralException.class)
                    .hasMessageContaining(ErrorStatus.PLACE_NOT_FOUND.getMessage());
        }
    }
}
