package umc.catchy.domain.mapping.memberCourse.service;

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
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.course.domain.CourseType;
import umc.catchy.domain.mapping.memberCourse.dao.MemberCourseRepository;
import umc.catchy.domain.mapping.memberCourse.domain.MemberCourse;
import umc.catchy.domain.mapping.memberCourse.dto.response.CourseBookmarkResponse;
import umc.catchy.domain.mapping.memberCourse.dto.response.MemberCourseResponse;
import umc.catchy.domain.mapping.memberCourse.dto.response.MemberCourseSliceResponse;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;
import umc.catchy.support.fixture.CourseFixture;
import umc.catchy.support.fixture.MemberCourseFixture;
import umc.catchy.support.fixture.MemberFixture;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberCourseServiceTest {

    @InjectMocks
    private MemberCourseService memberCourseService;

    @Mock
    private MemberCourseRepository memberCourseRepository;

    @Mock
    private MemberRepository memberRepository;

    private Member testMember;
    private Course testCourse;
    private MemberCourse testMemberCourse;

    @BeforeEach
    void setUp() {
        testMember = MemberFixture.createTestMember();
        testCourse = CourseFixture.createTestCourse(testMember);
        testMemberCourse = MemberCourseFixture.createTestMemberCourse(testMember, testCourse);
    }

    private void mockSecurityUtil(MockedStatic<SecurityUtil> mockedSecurityUtil) {
        mockedSecurityUtil.when(SecurityUtil::getCurrentMemberId).thenReturn(1L);
    }

    @Test
    @DisplayName("북마크 토글 성공 - 북마크 ON")
    void toggleBookmark_success_on() {
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            // given
            mockSecurityUtil(mockedSecurityUtil);

            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
            when(memberCourseRepository.findByCourseIdAndMemberId(1L, 1L))
                    .thenReturn(Optional.of(testMemberCourse));

            // when
            CourseBookmarkResponse result = memberCourseService.toggleBookmark(1L);

            // then
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.memberCourseId()).isEqualTo(1L),
                    () -> assertThat(result.bookmarked()).isTrue()
            );

            verify(memberRepository).findById(1L);
            verify(memberCourseRepository).findByCourseIdAndMemberId(1L, 1L);
        }
    }

    @Test
    @DisplayName("북마크 토글 실패 - 존재하지 않는 회원")
    void toggleBookmark_fail_memberNotFound() {
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            // given
            mockSecurityUtil(mockedSecurityUtil);

            when(memberRepository.findById(1L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberCourseService.toggleBookmark(100L))
                    .isInstanceOf(GeneralException.class)
                    .hasMessageContaining(ErrorStatus.MEMBER_NOT_FOUND.getMessage());

            verify(memberRepository).findById(1L);
        }
    }

    @Test
    @DisplayName("북마크 토글 실패 - 존재하지 않는 MemberCourse")
    void toggleBookmark_fail_memberCourseNotFound() {
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            // given
            mockSecurityUtil(mockedSecurityUtil);

            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
            when(memberCourseRepository.findByCourseIdAndMemberId(999L, 1L))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberCourseService.toggleBookmark(999L))
                    .isInstanceOf(GeneralException.class)
                    .hasMessageContaining(ErrorStatus.COURSE_MEMBER_NOT_FOUND.getMessage());

            verify(memberCourseRepository).findByCourseIdAndMemberId(999L, 1L);
        }
    }

    @Test
    @DisplayName("북마크된 코스 목록 조회 성공")
    void findAllCourseByBookmarked_success() {
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            // given
            mockSecurityUtil(mockedSecurityUtil);

            MemberCourseResponse responseDto = new MemberCourseResponse(
                    100L,
                    CourseType.AI,
                    "image.jpg",
                    "AI 코스",
                    "설명",
                    List.of("음식점", "카페")
            );

            Slice<MemberCourseResponse> slice = new SliceImpl<>(List.of(responseDto));

            when(memberCourseRepository.findCourseByBookmarks(eq(1L), eq(10), eq(null)))
                    .thenReturn(slice);

            // when
            MemberCourseSliceResponse result = memberCourseService.findAllCourseByBookmarked(10, null);

            // then
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.content()).hasSize(1),
                    () -> assertThat(result.content().get(0).courseName()).isEqualTo("AI 코스"),
                    () -> assertThat(result.isLast()).isTrue()
            );

            verify(memberCourseRepository).findCourseByBookmarks(eq(1L), eq(10), eq(null));
        }
    }

    @Test
    @DisplayName("북마크된 코스 목록 조회 성공 - 빈 결과")
    void findAllCourseByBookmarked_success_empty() {
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            // given
            mockSecurityUtil(mockedSecurityUtil);

            Slice<MemberCourseResponse> emptySlice = new SliceImpl<>(List.of());

            when(memberCourseRepository.findCourseByBookmarks(eq(1L), eq(10), eq(null)))
                    .thenReturn(emptySlice);

            // when
            MemberCourseSliceResponse result = memberCourseService.findAllCourseByBookmarked(10, null);

            // then
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.content()).isEmpty(),
                    () -> assertThat(result.isLast()).isTrue()
            );

            verify(memberCourseRepository).findCourseByBookmarks(eq(1L), eq(10), eq(null));
        }
    }

    @Test
    @DisplayName("내 코스 목록 조회 성공 - 필터링 및 페이징")
    void getMemberCourses_success() {
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            // given
            mockSecurityUtil(mockedSecurityUtil);

            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));

            MemberCourseResponse responseDto = new MemberCourseResponse(
                    100L,
                    CourseType.DIY,
                    null,
                    "테스트 코스",
                    "설명",
                    List.of("음식점", "카페")
            );

            Slice<MemberCourseResponse> slice = new SliceImpl<>(List.of(responseDto));

            CourseType courseType = CourseType.DIY;
            String upperLoc = "서울시";
            String lowerLoc = "강남구";
            Long lastId = 200L;

            when(memberCourseRepository.findCourseByFilters(
                    eq(courseType), eq(upperLoc), eq(lowerLoc), eq(1L), eq(lastId)
            )).thenReturn(slice);

            // when
            MemberCourseSliceResponse result = memberCourseService.getMemberCourses(courseType, upperLoc, lowerLoc, lastId);

            // then
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.content()).hasSize(1),
                    () -> assertThat(result.content().get(0).courseName()).isEqualTo("테스트 코스"),
                    () -> assertThat(result.isLast()).isTrue()
            );

            verify(memberCourseRepository).findCourseByFilters(
                    eq(courseType), eq(upperLoc), eq(lowerLoc), eq(1L), eq(lastId)
            );
        }
    }

    @Test
    @DisplayName("내 코스 목록 조회 성공 - 빈 결과")
    void getMemberCourses_success_empty() {
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            // given
            mockSecurityUtil(mockedSecurityUtil);

            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));

            Slice<MemberCourseResponse> emptySlice = new SliceImpl<>(List.of());

            CourseType courseType = CourseType.AI;
            String upperLoc = "all";
            String lowerLoc = "all";

            when(memberCourseRepository.findCourseByFilters(
                    eq(courseType), eq(upperLoc), eq(lowerLoc), eq(1L), eq(null)
            )).thenReturn(emptySlice);

            // when
            MemberCourseSliceResponse result = memberCourseService.getMemberCourses(courseType, upperLoc, lowerLoc, null);

            // then
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.content()).isEmpty(),
                    () -> assertThat(result.isLast()).isTrue()
            );

            verify(memberCourseRepository).findCourseByFilters(
                    eq(courseType), eq(upperLoc), eq(lowerLoc), eq(1L), eq(null)
            );
        }
    }
}
