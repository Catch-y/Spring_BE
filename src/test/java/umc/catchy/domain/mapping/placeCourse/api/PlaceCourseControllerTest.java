package umc.catchy.domain.mapping.placeCourse.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import umc.catchy.domain.mapping.placeCourse.dto.request.PlaceSearchRequest;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceDetailResponse;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlacePreviewResponse;
import umc.catchy.domain.mapping.placeCourse.service.PlaceCourseFacade;
import umc.catchy.domain.mapping.placeLike.dto.response.LikedPlaceSliceResponse;
import umc.catchy.support.ControllerTestSupport;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlaceCourseController.class)
class PlaceCourseControllerTest extends ControllerTestSupport {

    @MockitoBean
    private PlaceCourseFacade placeCourseFacade;

    @Test
    @DisplayName("프론트엔드 장소 검색 데이터 처리 - 성공")
    void searchPlacesByFrontend_success() throws Exception {
        PlaceSearchRequest request = new PlaceSearchRequest(100L, 37.5, 127.0, "테스트 장소", "서울시 강남구");
        PlacePreviewResponse response = new PlacePreviewResponse(
                1L, "테스트 장소", "image_url", "카페", "도로명주소", "09:00-22:00", 4.5, 37.5, 127.0, 10L, true
        );

        when(placeCourseFacade.getPlacesByFrontend(anyList())).thenReturn(List.of(response));

        mockMvc.perform(post("/course/place/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(request)))
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result[0].placeId").value(1L))
                .andExpect(jsonPath("$.result[0].placeName").value("테스트 장소"))
                .andExpect(jsonPath("$.result[0].placeImage").value("image_url"))
                .andExpect(jsonPath("$.result[0].placeLatitude").value(37.5))
                .andExpect(jsonPath("$.result[0].liked").value(true));
    }

    @Test
    @DisplayName("장소 상세 정보 조회 - 성공")
    void getPlaceDetail_success() throws Exception {
        Long placeId = 1L;
        PlaceDetailResponse response = new PlaceDetailResponse(
                placeId, "img_url", "장소이름", "장소설명", "카페", "도로명주소",
                "09:00-22:00", "https://site.com", 4.5, 100L, 37.5, 127.0, true, true
        );

        when(placeCourseFacade.getPlaceDetailByPlaceId(placeId)).thenReturn(response);

        mockMvc.perform(get("/course/place/{placeId}", placeId)
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.placeId").value(placeId))
                .andExpect(jsonPath("$.result.placeName").value("장소이름"))
                .andExpect(jsonPath("$.result.visited").value(true))
                .andExpect(jsonPath("$.result.liked").value(true));
    }

    @Test
    @DisplayName("좋아요한 장소 목록 무한 스크롤 조회 - 성공")
    void findAllCourseByBookmarked_success() throws Exception {
        LikedPlaceSliceResponse response = LikedPlaceSliceResponse.of(List.of(), true);

        when(placeCourseFacade.searchLikedPlace(anyInt(), anyLong())).thenReturn(response);

        mockMvc.perform(get("/course/place/mypage/like")
                        .param("pageSize", "10")
                        .param("lastPlaceId", "5")
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.content").isArray())
                .andExpect(jsonPath("$.result.last").value(true));
    }
}
