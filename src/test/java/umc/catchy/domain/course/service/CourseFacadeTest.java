package umc.catchy.domain.course.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.course.domain.CourseType;
import umc.catchy.domain.course.dto.request.CourseCreateRequest;
import umc.catchy.domain.course.dto.request.CourseUpdateRequest;
import umc.catchy.domain.course.dto.response.CourseDetailResponse;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;
import umc.catchy.infra.aws.s3.AmazonS3Manager;
import umc.catchy.support.fixture.CourseFixture;
import umc.catchy.support.fixture.MemberFixture;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static umc.catchy.support.fixture.MemberFixture.*;
import static umc.catchy.support.fixture.CourseFixture.*;

@ExtendWith(MockitoExtension.class)
class CourseFacadeTest {

    @InjectMocks
    private CourseFacade courseFacade;

    @Mock
    private CourseCommandService commandService;

    @Mock
    private CourseService courseService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AmazonS3Manager s3Manager;

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
    @DisplayName("코스 생성 성공 - 이미지 없음")
    void createCourse_success_without_image() {
        // given
        CourseCreateRequest request = new CourseCreateRequest(
                "새 코스",
                "새 설명",
                CourseType.DIY,
                List.of(1L, 2L, 3L),
                null,
                "09:00",
                "18:00"
        );

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockSecurityUtil(mockedSecurityUtil);

            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));

            Course savedCourse = createTestCourse(2L, "새 코스", testMember);
            when(commandService.saveCourse(any(Course.class))).thenReturn(savedCourse);

            CourseDetailResponse expectedResponse = CourseDetailResponse.from(
                    savedCourse, 0, false, List.of()
            );
            when(courseService.getCourseDetails(2L)).thenReturn(expectedResponse);

            // when
            CourseDetailResponse response = courseFacade.createCourse(request);

            // then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.courseName()).isEqualTo("새 코스")
            );

            verify(s3Manager, never()).uploadFile(anyString(), any());
            verify(commandService).saveCourse(any(Course.class));
            verify(commandService).registerPlacesToCourse(eq(savedCourse), eq(List.of(1L, 2L, 3L)));
            verify(commandService).saveMemberCourse(eq(savedCourse), eq(testMember));
            verify(courseService).getCourseDetails(2L);
        }
    }

    @Test
    @DisplayName("코스 생성 성공 - 이미지 있음")
    void createCourse_success_with_image() {
        // given
        MockMultipartFile imageFile = new MockMultipartFile(
                "courseImage",
                "course.jpg",
                "image/jpeg",
                "fake-image-data".getBytes()
        );

        CourseCreateRequest request = new CourseCreateRequest(
                "새 코스",
                "새 설명",
                CourseType.DIY,
                List.of(1L, 2L, 3L),
                imageFile,  // 이미지 있음
                "09:00",
                "18:00"
        );

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockSecurityUtil(mockedSecurityUtil);

            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
            when(s3Manager.uploadFile(anyString(), any(MultipartFile.class)))
                    .thenReturn("https://s3.aws.com/new-course.jpg");

            Course savedCourse = createTestCourse(2L, "새 코스", testMember);
            when(commandService.saveCourse(any(Course.class))).thenReturn(savedCourse);

            CourseDetailResponse expectedResponse = CourseDetailResponse.from(
                    savedCourse, 0, false, List.of()
            );
            when(courseService.getCourseDetails(2L)).thenReturn(expectedResponse);

            // when
            CourseDetailResponse response = courseFacade.createCourse(request);

            // then
            assertThat(response).isNotNull();

            // S3 업로드 검증
            verify(s3Manager).uploadFile(anyString(), eq(imageFile));
            verify(commandService).saveCourse(any(Course.class));
            verify(commandService).registerPlacesToCourse(eq(savedCourse), eq(List.of(1L, 2L, 3L)));
            verify(commandService).saveMemberCourse(eq(savedCourse), eq(testMember));
            verify(courseService).getCourseDetails(2L);
        }
    }

    @Test
    @DisplayName("코스 생성 실패 - S3 업로드 후 DB 실패 시 롤백")
    void createCourse_fail_rollback_s3() {
        // given
        MockMultipartFile imageFile = new MockMultipartFile(
                "courseImage",
                "course.jpg",
                "image/jpeg",
                "fake-image-data".getBytes()
        );

        CourseCreateRequest request = new CourseCreateRequest(
                "새 코스",
                "새 설명",
                CourseType.DIY,
                List.of(1L, 2L, 3L),
                imageFile,
                "09:00",
                "18:00"
        );

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockSecurityUtil(mockedSecurityUtil);

            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
            when(s3Manager.uploadFile(anyString(), any(MultipartFile.class)))
                    .thenReturn("https://s3.aws.com/uploaded.jpg");

            // DB 저장 실패
            when(commandService.saveCourse(any(Course.class)))
                    .thenThrow(new RuntimeException("DB Error"));

            // when & then
            assertThatThrownBy(() -> courseFacade.createCourse(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("DB Error");

            // S3 롤백 검증
            verify(s3Manager).uploadFile(anyString(), any());
            verify(s3Manager).deleteImage("https://s3.aws.com/uploaded.jpg");
            verify(courseService, never()).getCourseDetails(anyLong());
        }
    }

    @Test
    @DisplayName("코스 생성 실패 - 회원 없음")
    void createCourse_fail_member_not_found() {
        // given
        CourseCreateRequest request = new CourseCreateRequest(
                "새 코스",
                "새 설명",
                CourseType.DIY,
                List.of(1L, 2L, 3L),
                null,
                "09:00",
                "18:00"
        );

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockSecurityUtil(mockedSecurityUtil);

            when(memberRepository.findById(1L))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> courseFacade.createCourse(request))
                    .isInstanceOf(GeneralException.class)
                    .hasMessageContaining(ErrorStatus.MEMBER_NOT_FOUND.getMessage());

            verify(commandService, never()).saveCourse(any());
        }
    }

    @Test
    @DisplayName("코스 수정 성공 - 이미지 변경 없음")
    void updateCourse_success_without_image_change() {
        // given
        Long courseId = 1L;
        CourseUpdateRequest request = new CourseUpdateRequest(
                "수정된 코스명",
                "수정된 설명",
                List.of(4L, 5L, 6L),
                null,  // 이미지 변경 없음
                "10:00",
                "19:00"
        );

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockSecurityUtil(mockedSecurityUtil);

            when(courseService.getCourse(courseId)).thenReturn(testCourse);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));

            CourseDetailResponse expectedResponse = CourseDetailResponse.from(
                    testCourse, 0, false, List.of()
            );
            when(courseService.getCourseDetails(courseId)).thenReturn(expectedResponse);

            // when
            CourseDetailResponse response = courseFacade.updateCourse(courseId, request);

            // then
            assertThat(response).isNotNull();
            verify(s3Manager, never()).deleteImage(anyString());
            verify(s3Manager, never()).uploadFile(anyString(), any());
            verify(commandService).updateCourse(
                    eq(testCourse),
                    eq("수정된 코스명"),
                    eq("수정된 설명"),
                    isNull(),
                    eq(List.of(4L, 5L, 6L)),
                    any(LocalTime.class),
                    any(LocalTime.class)
            );
        }
    }

    @Test
    @DisplayName("코스 수정 성공 - 이미지 변경")
    void updateCourse_success_with_image_change() {
        // given
        Long courseId = 1L;
        MockMultipartFile newImageFile = new MockMultipartFile(
                "courseImage",
                "new-course.jpg",
                "image/jpeg",
                "new-image-data".getBytes()
        );

        CourseUpdateRequest request = new CourseUpdateRequest(
                "수정된 코스명",
                "수정된 설명",
                List.of(4L, 5L, 6L),
                newImageFile,
                "10:00",
                "19:00"
        );

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockSecurityUtil(mockedSecurityUtil);

            when(courseService.getCourse(courseId)).thenReturn(testCourse);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
            when(s3Manager.uploadFile(anyString(), any(MultipartFile.class)))
                    .thenReturn("https://s3.aws.com/new-course-image.jpg");

            CourseDetailResponse expectedResponse = CourseDetailResponse.from(
                    testCourse, 0, false, List.of()
            );
            when(courseService.getCourseDetails(courseId)).thenReturn(expectedResponse);

            // when
            CourseDetailResponse response = courseFacade.updateCourse(courseId, request);

            // then
            assertThat(response).isNotNull();

            // 기존 이미지 삭제 검증
            verify(s3Manager).deleteImage("https://s3.aws.com/course.jpg");
            // 새 이미지 업로드 검증
            verify(s3Manager).uploadFile(anyString(), eq(newImageFile));
            verify(commandService).updateCourse(
                    eq(testCourse),
                    eq("수정된 코스명"),
                    eq("수정된 설명"),
                    eq("https://s3.aws.com/new-course-image.jpg"),
                    eq(List.of(4L, 5L, 6L)),
                    any(LocalTime.class),
                    any(LocalTime.class)
            );
        }
    }

    @Test
    @DisplayName("코스 수정 성공 - 기존 이미지가 없는 코스에 새 이미지 추가")
    void updateCourse_success_add_image_to_no_image_course() {
        // given
        Long courseId = 1L;
        Course noImageCourse = createTestCourseWithNoImage(courseId, "이미지 없는 코스", testMember);

        MockMultipartFile newImageFile = new MockMultipartFile("image", "new.jpg", "image/jpeg", "data".getBytes());

        CourseUpdateRequest request = new CourseUpdateRequest(
                "수정", "설명", List.of(4L), newImageFile, "10:00", "19:00"
        );

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockSecurityUtil(mockedSecurityUtil);

            when(courseService.getCourse(courseId)).thenReturn(noImageCourse);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
            when(s3Manager.uploadFile(anyString(), any())).thenReturn("new-url.jpg");
            when(courseService.getCourseDetails(courseId)).thenReturn(mock(CourseDetailResponse.class));

            // when
            courseFacade.updateCourse(courseId, request);

            // then
            // 기존 이미지가 없으므로 deleteImage는 호출되면 안 됨
            verify(s3Manager, never()).deleteImage(anyString());
            // 업로드는 되어야 함
            verify(s3Manager).uploadFile(anyString(), eq(newImageFile));
        }
    }

    @Test
    @DisplayName("코스 수정 실패 - 권한 없음 (다른 사람의 코스)")
    void updateCourse_fail_invalid_member() {
        // given
        Long courseId = 1L;
        CourseUpdateRequest request = new CourseUpdateRequest(
                "수정 시도",
                "설명",
                List.of(1L, 2L),
                null,
                "09:00",
                "18:00"
        );

        Member otherMember = createTestMember(2L, "other@test.com");
        Course otherMemberCourse = createTestCourse(1L, "다른사람 코스", otherMember);

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockSecurityUtil(mockedSecurityUtil);

            when(courseService.getCourse(courseId)).thenReturn(otherMemberCourse);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));

            // when & then
            assertThatThrownBy(() -> courseFacade.updateCourse(courseId, request))
                    .isInstanceOf(GeneralException.class)
                    .hasMessageContaining(ErrorStatus.COURSE_INVALID_MEMBER.getMessage());

            verify(s3Manager, never()).uploadFile(anyString(), any());
            verify(commandService, never()).updateCourse(any(), any(), any(), any(), any(), any(), any());
        }
    }

    @Test
    @DisplayName("코스 수정 실패 - S3 업로드 실패 시 롤백")
    void updateCourse_fail_rollback_s3() {
        // given
        Long courseId = 1L;
        MockMultipartFile newImageFile = new MockMultipartFile(
                "courseImage",
                "new.jpg",
                "image/jpeg",
                "data".getBytes()
        );

        CourseUpdateRequest request = new CourseUpdateRequest(
                "수정",
                "설명",
                List.of(1L, 2L),
                newImageFile,
                "09:00",
                "18:00"
        );

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockSecurityUtil(mockedSecurityUtil);

            when(courseService.getCourse(courseId)).thenReturn(testCourse);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
            when(s3Manager.uploadFile(anyString(), any(MultipartFile.class)))
                    .thenReturn("https://s3.aws.com/new-image.jpg");

            // DB 업데이트 실패
            doThrow(new RuntimeException("DB Update Error"))
                    .when(commandService).updateCourse(any(), any(), any(), any(), any(), any(), any());

            // when & then
            assertThatThrownBy(() -> courseFacade.updateCourse(courseId, request))
                    .isInstanceOf(RuntimeException.class);

            // 기존 이미지 삭제 + 새 이미지 업로드 + 롤백 검증
            verify(s3Manager).deleteImage("https://s3.aws.com/course.jpg");
            verify(s3Manager).uploadFile(anyString(), any());
            verify(s3Manager).deleteImage("https://s3.aws.com/new-image.jpg");
        }
    }

    @Test
    @DisplayName("코스 수정 실패 - 잘못된 시간 포맷")
    void updateCourse_fail_invalid_time_format() {
        // given
        Long courseId = 1L;
        CourseUpdateRequest request = new CourseUpdateRequest(
                "수정", "설명", List.of(1L), null,
                "25:00", "18:00" // 잘못된 시간
        );

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockSecurityUtil(mockedSecurityUtil);

            when(courseService.getCourse(courseId)).thenReturn(testCourse);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));

            // when & then
            assertThatThrownBy(() -> courseFacade.updateCourse(courseId, request))
                    .isInstanceOf(java.time.format.DateTimeParseException.class);
        }
    }

    @Test
    @DisplayName("코스 삭제 성공")
    void deleteCourse_success() {
        // given
        Long courseId = 1L;

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockSecurityUtil(mockedSecurityUtil);

            when(courseService.getCourse(courseId)).thenReturn(testCourse);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));

            // when
            courseFacade.deleteCourse(courseId);

            // then
            verify(courseService).getCourse(courseId);
            verify(commandService).deleteCourse(testCourse, testMember);
        }
    }

    @Test
    @DisplayName("코스 삭제 실패 - 코스 없음")
    void deleteCourse_fail_course_not_found() {
        // given
        Long courseId = 999L;

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockSecurityUtil(mockedSecurityUtil);

            when(courseService.getCourse(courseId))
                    .thenThrow(new GeneralException(ErrorStatus.COURSE_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> courseFacade.deleteCourse(courseId))
                    .isInstanceOf(GeneralException.class)
                    .hasMessageContaining(ErrorStatus.COURSE_NOT_FOUND.getMessage());

            verify(commandService, never()).deleteCourse(any(), any());
        }
    }

    @Test
    @DisplayName("코스 삭제 실패 - 권한 없음 (다른 사람의 코스)")
    void deleteCourse_fail_invalid_member() {
        // given
        Long courseId = 1L;
        Member otherMember = createTestMember(2L, "other@test.com");
        Course otherMemberCourse = createTestCourse(1L, "남의 코스", otherMember);

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockSecurityUtil(mockedSecurityUtil);

            when(courseService.getCourse(courseId)).thenReturn(otherMemberCourse);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));

            doThrow(new GeneralException(ErrorStatus.COURSE_INVALID_MEMBER))
                    .when(commandService).deleteCourse(otherMemberCourse, testMember);

            // when & then
            assertThatThrownBy(() -> courseFacade.deleteCourse(courseId))
                    .isInstanceOf(GeneralException.class)
                    .hasMessageContaining(ErrorStatus.COURSE_INVALID_MEMBER.getMessage());
        }
    }
}
