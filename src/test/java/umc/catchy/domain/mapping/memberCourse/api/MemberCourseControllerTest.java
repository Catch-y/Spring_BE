package umc.catchy.domain.mapping.memberCourse.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import umc.catchy.domain.course.domain.CourseType;
import umc.catchy.domain.mapping.memberCourse.dto.response.CourseBookmarkResponse;
import umc.catchy.domain.mapping.memberCourse.dto.response.MemberCourseResponse;
import umc.catchy.domain.mapping.memberCourse.service.MemberCourseService;
import umc.catchy.global.common.dto.SliceResponse;
import umc.catchy.support.ControllerTestSupport;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberCourseController.class)
class MemberCourseControllerTest extends ControllerTestSupport {

    @MockitoBean
    private MemberCourseService memberCourseService;

    @Test
    @DisplayName("내 코스 조회 API (검색/필터링) - 성공")
    void getMemberCourses_success() throws Exception {
        // given
        MemberCourseResponse content = new MemberCourseResponse(
                100L,
                CourseType.DIY,
                null,
                "테스트 코스",
                null,
                List.of("음식점", "카페")
        );

        SliceResponse<MemberCourseResponse> response = new SliceResponse<>(List.of(content), true);

        CourseType type = CourseType.DIY;
        String upperLoc = "서울시";
        String lowerLoc = "강남구";
        Long lastId = 10L;

        when(memberCourseService.getMemberCourses(eq(type), eq(upperLoc), eq(lowerLoc), eq(lastId)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/course/search")
                        .header("Authorization", testToken)
                        .param("type", "DIY")
                        .param("upperLocation", "서울시")
                        .param("lowerLocation", "강남구")
                        .param("lastId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.content[0].courseName").value("테스트 코스"))
                .andExpect(jsonPath("$.result.isLast").value(true));
    }

    @Test
    @DisplayName("코스 북마크 토글 API - 성공 (북마크 해제)")
    void toggleBookmark_success_unbookmark() throws Exception {
        // given
        Long courseId = 1L;

        CourseBookmarkResponse response = new CourseBookmarkResponse(
                100L,
                false
        );

        when(memberCourseService.toggleBookmark(courseId)).thenReturn(response);

        // when & then
        mockMvc.perform(patch("/course/{courseId}/bookmark", courseId)
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.memberCourseId").value(100L))
                .andExpect(jsonPath("$.result.bookmarked").value(false));
    }

    @Test
    @DisplayName("북마크된 코스 목록 조회 - 성공")
    void findAllCourseByBookmarked_success() throws Exception {
        // given
        MemberCourseResponse content = new MemberCourseResponse(
                200L,
                CourseType.AI,
                "image.jpg",
                "AI 추천 코스",
                "AI가 추천한 코스입니다.",
                List.of("관광지", "음식점")
        );

        SliceResponse<MemberCourseResponse> response = new SliceResponse<>(List.of(content), false);

        when(memberCourseService.findAllCourseByBookmarked(eq(10), eq(null)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/mypage/bookmark")
                        .header("Authorization", testToken)
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.content[0].courseName").value("AI 추천 코스"))
                .andExpect(jsonPath("$.result.isLast").value(false));
    }
}
