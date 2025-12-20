package umc.catchy.domain.course.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import umc.catchy.domain.course.dto.request.CourseCreateRequest;
import umc.catchy.domain.course.dto.request.CourseUpdateRequest;
import umc.catchy.domain.course.dto.response.CourseDetailResponse;
import umc.catchy.domain.course.dto.response.GptCourseInfoResponse;
import umc.catchy.domain.course.service.AICourseGenerationService;
import umc.catchy.domain.course.service.CourseFacade;
import umc.catchy.domain.course.service.CourseRecommendationService;
import umc.catchy.domain.course.service.CourseService;
import umc.catchy.domain.courseReview.dto.request.PostCourseReviewRequest;
import umc.catchy.domain.courseReview.dto.response.PostCourseReviewResponse;
import umc.catchy.domain.courseReview.service.CourseReviewService;
import umc.catchy.domain.mapping.memberCourse.service.MemberCourseService;
import umc.catchy.domain.mapping.placeVisit.service.PlaceVisitService;
import umc.catchy.domain.place.dto.request.SetCategoryRequest;
import umc.catchy.domain.place.service.PlaceService;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;
import umc.catchy.support.ControllerTestSupport;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

@WebMvcTest(CourseController.class)
class CourseControllerTest extends ControllerTestSupport {

    @MockitoBean
    private CourseService courseService;

    @MockitoBean
    private CourseFacade courseFacade;

    @MockitoBean
    private AICourseGenerationService aiCourseGenerationService;

    @MockitoBean
    private CourseRecommendationService courseRecommendationService;

    @MockitoBean
    private CourseReviewService courseReviewService;

    @MockitoBean
    private MemberCourseService memberCourseService;

    @MockitoBean
    private PlaceService placeService;

    @MockitoBean
    private PlaceVisitService placeVisitService;

    @Test
    @DisplayName("코스 상세 조회 API - 성공")
    void getCourseInfo_success() throws Exception {
        // given
        Long courseId = 1L;
        CourseDetailResponse response = mock(CourseDetailResponse.class);

        when(courseService.getCourseDetails(courseId)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/course/detail/{courseId}", courseId)
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));
    }

    @Test
    @DisplayName("코스 생성 API - 성공 (Multipart)")
    void createCourse_success() throws Exception {
        // given
        CourseDetailResponse response = mock(CourseDetailResponse.class);

        MockMultipartFile image = new MockMultipartFile(
                "courseImage",
                "test-image.jpg",
                "image/jpeg",
                "dummy-data".getBytes()
        );

        when(courseFacade.createCourse(any(CourseCreateRequest.class)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(multipart("/course")
                        .file(image)
                        .param("courseName", "나만의 힐링 코스")
                        .param("courseDescription", "테스트 코스입니다.")
                        .param("courseType", "DIY")
                        .param("recommendTimeStart", "09:00")
                        .param("recommendTimeEnd", "18:00")
                        .param("placeIds", "1", "2", "3")
                        .header("Authorization", testToken)
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));
    }

    @Test
    @DisplayName("코스 수정 API - 성공 (PATCH Multipart)")
    void updateCourse_success() throws Exception {
        // given
        Long courseId = 1L;
        CourseDetailResponse response = mock(CourseDetailResponse.class);

        MockMultipartFile image = new MockMultipartFile(
                "courseImage",
                "update.jpg",
                "image/jpeg",
                "update-data".getBytes()
        );

        when(courseFacade.updateCourse(eq(courseId), any(CourseUpdateRequest.class)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(multipart("/course/{courseId}", courseId)
                        .file(image)
                        .param("courseName", "수정된 코스")
                        .param("placeIds", "4", "5")
                        .header("Authorization", testToken)
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));
    }

    @Test
    @DisplayName("AI 코스 생성 API - 성공 (비동기 처리)")
    void generateCourseWithAI_success() throws Exception {
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            // given
            mockedSecurityUtil.when(SecurityUtil::getCurrentMemberId).thenReturn(1L);

            GptCourseInfoResponse response = new GptCourseInfoResponse(
                    "AI 생성 코스",
                    "AI가 추천하는 설명",
                    "10:00 ~ 20:00",
                    List.of()
            );

            when(aiCourseGenerationService.generateCourseAutomatically(1L))
                    .thenReturn(CompletableFuture.completedFuture(response));

            // when
            MvcResult mvcResult = mockMvc.perform(post("/course/generate-ai")
                            .header("Authorization", testToken))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.courseName").value("AI 생성 코스"));

            // then
            verify(aiCourseGenerationService).increaseGptCount(1L);
        }
    }

    @Test
    @DisplayName("AI 코스 생성 실패 - 비동기 예외 발생")
    void generateCourseWithAI_fail_exception() throws Exception {
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            // given
            mockedSecurityUtil.when(SecurityUtil::getCurrentMemberId).thenReturn(1L);

            when(aiCourseGenerationService.generateCourseAutomatically(1L))
                    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("GPT Error")));

            // when
            MvcResult mvcResult = mockMvc.perform(post("/course/generate-ai")
                            .header("Authorization", testToken))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            // then
            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("GPT404"))
                    .andExpect(jsonPath("$.message").value("GPT 호출에 실패했습니다."));
        }
    }

    @Test
    @DisplayName("코스 리뷰 작성 - 이미지 없이 요청 (Null Handling)")
    void postCourseReview_success_no_image() throws Exception {
        // given
        Long courseId = 1L;

        PostCourseReviewResponse.newCourseReviewResponseDTO response = PostCourseReviewResponse.newCourseReviewResponseDTO.builder()
                .reviewId(100L)
                .comment("좋았습니다.")
                .reviewImages(List.of()) // 이미지 없음
                .createdAt(LocalDate.now())
                .creatorNickname("TestUser")
                .build();

        when(courseReviewService.postNewCourseReview(eq(courseId), any(PostCourseReviewRequest.class)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(multipart("/course/{courseId}/review", courseId)
                        .param("comment", "좋았습니다.") // DTO 필드명에 맞춰 param 이름 comment로 사용
                        .param("rating", "5.0")
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.reviewId").value(100L))
                .andExpect(jsonPath("$.result.comment").value("좋았습니다."));

        verify(courseReviewService).postNewCourseReview(eq(courseId), any(PostCourseReviewRequest.class));
    }

    @Test
    @DisplayName("장소 카테고리 선택 - 성공 (@RequestBody)")
    void selectCategories_success() throws Exception {
        // given
        Long placeId = 10L;

        SetCategoryRequest request = new SetCategoryRequest(
                "음식점",
                "한식"
        );

        // when & then
        mockMvc.perform(post("/course/{placeId}", placeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));

        verify(placeService).setCategories(eq(placeId), any(SetCategoryRequest.class));
    }

    @Test
    @DisplayName("코스 삭제 API - 성공")
    void deleteCourse_success() throws Exception {
        // given
        Long courseId = 1L;

        // when & then
        mockMvc.perform(delete("/course/{courseId}", courseId)
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));

        verify(courseFacade).deleteCourse(courseId);
    }

    @Test
    @DisplayName("코스 삭제 실패 - 존재하지 않는 코스")
    void deleteCourse_fail_notFound() throws Exception {
        // given
        Long courseId = 999L;

        doThrow(new GeneralException(ErrorStatus.COURSE_NOT_FOUND))
                .when(courseFacade).deleteCourse(courseId);

        // when & then
        mockMvc.perform(delete("/course/{courseId}", courseId)
                        .header("Authorization", testToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COURSE404"))
                .andExpect(jsonPath("$.message").value("해당 코스를 찾을 수 없습니다."));
    }
}
