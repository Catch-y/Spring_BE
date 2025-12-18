package umc.catchy.domain.course.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.catchy.domain.course.dao.CourseRepository;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.mapping.memberCourse.dao.MemberCourseRepository;
import umc.catchy.domain.mapping.memberCourse.domain.MemberCourse;
import umc.catchy.domain.mapping.placeCourse.dao.PlaceCourseRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.dao.PlaceRepository;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.support.fixture.PlaceFixture;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static umc.catchy.support.fixture.CourseFixture.createTestCourse;
import static umc.catchy.support.fixture.MemberFixture.createTestMember;

@ExtendWith(MockitoExtension.class)
class CourseCommandServiceTest {

    @InjectMocks
    private CourseCommandService commandService;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private PlaceCourseRepository placeCourseRepository;

    @Mock
    private MemberCourseRepository memberCourseRepository;

    @Mock
    private PlaceRepository placeRepository;

    private Member testMember;
    private Course testCourse;

    @BeforeEach
    void setUp() {
        testMember = createTestMember();
        testCourse = createTestCourse(testMember);
    }

    @Test
    @DisplayName("코스 저장 성공")
    void saveCourse_success() {
        // given
        when(courseRepository.save(any(Course.class))).thenReturn(testCourse);

        // when
        Course savedCourse = commandService.saveCourse(testCourse);

        // then
        assertAll(
                () -> assertThat(savedCourse).isNotNull(),
                () -> assertThat(savedCourse.getCourseName()).isEqualTo(testCourse.getCourseName())
        );

        verify(courseRepository).save(any(Course.class));
    }

    @Test
    @DisplayName("장소 등록 및 평점 계산 성공")
    void registerPlacesToCourse_success() {
        // given
        List<Long> placeIds = List.of(1L, 2L, 3L);

        Place place1 = PlaceFixture.createPlace(1L, 5.0);
        Place place2 = PlaceFixture.createPlace(2L, 3.0);
        Place place3 = PlaceFixture.createPlace(3L, 0.0);

        when(placeRepository.findAllById(placeIds)).thenReturn(List.of(place1, place2, place3));

        // when
        commandService.registerPlacesToCourse(testCourse, placeIds);

        // then
        assertThat(testCourse.getRating()).isEqualTo(4.0);

        verify(placeRepository).findAllById(placeIds);
        verify(placeCourseRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("유저-코스 매핑 저장 성공")
    void saveMemberCourse_success() {
        // given
        MemberCourse memberCourse = MemberCourse.builder()
                .member(testMember)
                .course(testCourse)
                .build();

        when(memberCourseRepository.save(any(MemberCourse.class))).thenReturn(memberCourse);

        // when
        MemberCourse result = commandService.saveMemberCourse(testCourse, testMember);

        // then
        assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getMember()).isEqualTo(testMember),
                () -> assertThat(result.getCourse()).isEqualTo(testCourse)
        );

        verify(memberCourseRepository).save(any(MemberCourse.class));
    }

    @Test
    @DisplayName("코스 수정 성공 - 기본 정보 및 장소 목록 갱신")
    void updateCourse_success() {
        // given
        String newName = "수정된 이름";
        String newDesc = "수정된 설명";
        String newImage = "https://new-image.jpg";
        List<Long> newPlaceIds = List.of(4L, 5L);
        LocalTime newStart = LocalTime.of(10, 0);
        LocalTime newEnd = LocalTime.of(20, 0);

        when(placeCourseRepository.findAllByCourse(testCourse)).thenReturn(List.of());

        when(placeRepository.findAllById(newPlaceIds)).thenReturn(List.of());

        // when
        commandService.updateCourse(
                testCourse, newName, newDesc, newImage,
                newPlaceIds, newStart, newEnd
        );

        // then
        assertAll(
                () -> assertThat(testCourse.getCourseName()).isEqualTo(newName),
                () -> assertThat(testCourse.getCourseDescription()).isEqualTo(newDesc),
                () -> assertThat(testCourse.getCourseImage()).isEqualTo(newImage),
                () -> assertThat(testCourse.getRecommendTimeStart()).isEqualTo(newStart),
                () -> assertThat(testCourse.getRecommendTimeEnd()).isEqualTo(newEnd)
        );

        verify(placeCourseRepository).deleteAll(anyList());
        verify(placeCourseRepository).flush();
        verify(placeRepository).findAllById(newPlaceIds);
        verify(placeCourseRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("코스 삭제 성공 - 연관 데이터 모두 삭제")
    void deleteCourse_success() {
        // given
        MemberCourse mockMemberCourse = MemberCourse.builder()
                .course(testCourse)
                .member(testMember)
                .build();

        when(memberCourseRepository.findByCourseAndMember(testCourse, testMember))
                .thenReturn(java.util.Optional.of(mockMemberCourse));

        when(placeCourseRepository.findAllByCourse(testCourse)).thenReturn(List.of());

        // when
        commandService.deleteCourse(testCourse, testMember);

        // then
        verify(memberCourseRepository).delete(mockMemberCourse);
        verify(placeCourseRepository).deleteAll(anyList());
        verify(courseRepository).delete(testCourse);
    }

    @Test
    @DisplayName("코스 삭제 실패 - 권한 없음 (소유자 아님)")
    void deleteCourse_fail_invalid_member() {
        // given
        when(memberCourseRepository.findByCourseAndMember(testCourse, testMember))
                .thenReturn(java.util.Optional.empty());

        // when & then
        assertThatThrownBy(() -> commandService.deleteCourse(testCourse, testMember))
                .isInstanceOf(GeneralException.class)
                .hasMessageContaining(ErrorStatus.COURSE_INVALID_MEMBER.getMessage());

        verify(courseRepository, never()).delete(any(Course.class));
        verify(placeCourseRepository, never()).deleteAll(anyList());
    }
}
