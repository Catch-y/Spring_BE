package umc.catchy.domain.member.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import umc.catchy.domain.activetime.dto.ActiveTimeRequest;
import umc.catchy.domain.category.dto.request.CategorySurveyRequest;
import umc.catchy.domain.location.dto.request.LocationSurveyRequest;
import umc.catchy.domain.mapping.memberCategory.dto.response.MemberCategoryCreatedResponse;
import umc.catchy.domain.mapping.memberLocation.dto.response.MemberLocationCreatedResponse;
import umc.catchy.domain.member.dto.request.StyleAndActiveTimeSurveyRequest;
import umc.catchy.domain.member.dto.response.StyleAndActiveTimeSurveyCreatedResponse;
import umc.catchy.domain.member.service.MemberSurveyService;
import umc.catchy.domain.style.domain.StyleName;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.support.ControllerTestSupport;

import java.time.DayOfWeek;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberSurveyController.class)
public class MemberSurveyControllerTest extends ControllerTestSupport {

    @MockitoBean
    private MemberSurveyService memberSurveyService;

    @Test
    @DisplayName("취향 설문 (카테고리) 저장 - 성공")
    void createMemberCategory_success() throws Exception {
        // given
        CategorySurveyRequest request = new CategorySurveyRequest(List.of("음식점", "카페"));
        MemberCategoryCreatedResponse response = new MemberCategoryCreatedResponse(List.of(1L, 2L));

        when(memberSurveyService.createMemberCategory(any(CategorySurveyRequest.class)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/member/survey/category")
                        .header("Authorization", testToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON201"))
                .andExpect(jsonPath("$.result.memberCategoryIds[0]").value(1L));
    }

    @Test
    @DisplayName("취향 설문 (스타일/시간) 저장 - 성공")
    void createMemberStyleTime_success() throws Exception {
        // given
        List<StyleName> styles = List.of(StyleName.ALONE, StyleName.FRIENDS);
        List<ActiveTimeRequest> times = List.of(new ActiveTimeRequest(DayOfWeek.MONDAY, "10:00", "12:00"));

        StyleAndActiveTimeSurveyRequest request = new StyleAndActiveTimeSurveyRequest(styles, times);

        StyleAndActiveTimeSurveyCreatedResponse response = new StyleAndActiveTimeSurveyCreatedResponse(
                List.of(10L, 11L),
                List.of(20L)
        );

        when(memberSurveyService.createStyleAndActiveTimeSurvey(any(StyleAndActiveTimeSurveyRequest.class)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/member/survey/styletime")
                        .header("Authorization", testToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON201"))
                .andExpect(jsonPath("$.result.memberStyleSurveyId").isArray())
                .andExpect(jsonPath("$.result.activeTimeSurveyId").isArray());
    }

    @Test
    @DisplayName("취향 설문 (스타일/시간) 저장 - 실패 (필수값 누락)")
    void createMemberStyleTime_fail_validation() throws Exception {
        // given
        StyleAndActiveTimeSurveyRequest request = new StyleAndActiveTimeSurveyRequest(null, null);

        ArgumentMatcher<StyleAndActiveTimeSurveyRequest> isInvalid =
                req -> req.styleNames() == null && req.activeTimes() == null;

        doThrow(new GeneralException(ErrorStatus._BAD_REQUEST))
                .when(memberSurveyService).createStyleAndActiveTimeSurvey(argThat(isInvalid));

        // when & then
        mockMvc.perform(post("/member/survey/styletime")
                        .header("Authorization", testToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
    }

    @Test
    @DisplayName("취향 설문 (선호지역) 저장 - 성공")
    void createMemberLocation_success() throws Exception {
        // given
        List<LocationSurveyRequest> requests = List.of(
                new LocationSurveyRequest("서울시", "강남구"),
                new LocationSurveyRequest("서울시", "마포구")
        );

        MemberLocationCreatedResponse response = new MemberLocationCreatedResponse(List.of(100L, 101L));

        when(memberSurveyService.createMemberLocation(any()))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/member/survey/location")
                        .header("Authorization", testToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.memberLocationId").isArray())
                .andExpect(jsonPath("$.result.memberLocationId[0]").value(100L));
    }
}
