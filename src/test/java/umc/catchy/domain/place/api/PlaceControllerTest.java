package umc.catchy.domain.place.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlacePreviewResponse;
import umc.catchy.domain.mapping.placeLike.dto.response.PlaceLikedResponse;
import umc.catchy.domain.mapping.placeVisit.dto.response.PlaceVisitedDateResponse;
import umc.catchy.domain.place.dto.response.PlaceSearchSliceResponse;
import umc.catchy.domain.place.service.PlaceService;
import umc.catchy.domain.placeReview.dto.request.PostPlaceReviewRequest;
import umc.catchy.domain.placeReview.dto.response.PostPlaceReviewResponse;
import umc.catchy.domain.placeReview.service.PlaceReviewService;
import umc.catchy.global.common.dto.SliceResponse;
import umc.catchy.support.ControllerTestSupport;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlaceController.class)
class PlaceControllerTest extends ControllerTestSupport {

    @MockitoBean
    private PlaceReviewService placeReviewService;

    @MockitoBean
    private PlaceService placeService;

    @Test
    @DisplayName("장소 평점/리뷰 등록 - 성공")
    void postPlaceReview_success() throws Exception {
        Long placeId = 1L;
        MockMultipartFile image = new MockMultipartFile("images", "test.jpg", "image/jpeg", "image".getBytes());

        PostPlaceReviewResponse.newPlaceReviewResponseDTO response = PostPlaceReviewResponse.newPlaceReviewResponseDTO.builder()
                .reviewId(100L)
                .comment("정말 좋아요")
                .rating(5)
                .visitedDate(LocalDate.of(2025, 12, 20))
                .creatorNickname("테스트유저")
                .build();

        when(placeReviewService.postNewPlaceReview(any(PostPlaceReviewRequest.class), eq(placeId)))
                .thenReturn(response);

        mockMvc.perform(multipart("/place/{placeId}/review", placeId)
                        .file(image)
                        .param("rating", "5")
                        .param("comment", "정말 좋아요")
                        .param("visitedDate", "2025-12-20")
                        .header("Authorization", testToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.reviewId").value(100L))
                .andExpect(jsonPath("$.result.creatorNickname").value("테스트유저"));
    }

    @Test
    @DisplayName("장소 좋아요 토글 - 성공")
    void toggleLike_success() throws Exception {
        Long placeId = 1L;
        PlaceLikedResponse response = PlaceLikedResponse.of(10L, true);

        when(placeService.togglePlaceLike(placeId)).thenReturn(response);

        mockMvc.perform(patch("/place/{placeId}/like", placeId)
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.liked").value(true));
    }

    @Test
    @DisplayName("장소 방문 날짜 리스트 조회 - 성공")
    void getPlaceVisitDate_success() throws Exception {
        Long courseId = 1L;
        Long placeId = 1L;
        PlaceVisitedDateResponse response = PlaceVisitedDateResponse.of(List.of(LocalDate.now()));

        when(placeService.getPlaceVisitDate(courseId, placeId)).thenReturn(response);

        mockMvc.perform(get("/place/{courseId}/{placeId}/visit", courseId, placeId)
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.visitedDate").isArray());
    }

    @Test
    @DisplayName("장소 리뷰 전체 조회 - 성공")
    void getAllPlaceReviews_success() throws Exception {
        Long placeId = 1L;
        PostPlaceReviewResponse.placeReviewAllResponseDTO response = PostPlaceReviewResponse.placeReviewAllResponseDTO.builder()
                .averageRating(4.5f)
                .totalCount(10L)
                .content(List.of())
                .last(true)
                .build();

        when(placeReviewService.getAllPlaceReviews(eq(placeId), anyInt(), any(), any()))
                .thenReturn(response);

        mockMvc.perform(get("/place/{placeId}/review/all", placeId)
                        .param("pageSize", "10")
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.averageRating").value(4.5))
                .andExpect(jsonPath("$.result.content").isArray())
                .andExpect(jsonPath("$.result.last").value(true));
    }

    @Test
    @DisplayName("사용자 장소 추천 조회 - 성공")
    void getRecommendPlaces_success() throws Exception {
        SliceResponse<PlacePreviewResponse> response = new SliceResponse<>(List.of(), true);

        when(placeService.recommendPlaces(anyDouble(), anyDouble(), anyInt(), anyInt()))
                .thenReturn(response);

        mockMvc.perform(get("/place/home/recommend-places")
                        .param("latitude", "37.5665")
                        .param("longitude", "126.9780")
                        .param("pageSize", "10")
                        .param("page", "0")
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.content").isArray());
    }

    @Test
    @DisplayName("장소 검색 - 성공")
    void getSearchPlaces_success() throws Exception {
        PlaceSearchSliceResponse response = PlaceSearchSliceResponse.of(List.of(), true);

        when(placeService.searchPlaceByCategoryOrName(anyInt(), anyString(), any(), any()))
                .thenReturn(response);

        mockMvc.perform(get("/place/home/search")
                        .param("keyword", "카페")
                        .param("pageSize", "10")
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.content").isArray());
    }
}
