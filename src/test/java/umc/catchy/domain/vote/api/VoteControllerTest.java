package umc.catchy.domain.vote.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.vote.domain.Vote;
import umc.catchy.domain.vote.dto.request.CategoryVoteRequest;
import umc.catchy.domain.vote.dto.request.PlaceVoteRequest;
import umc.catchy.domain.vote.dto.request.VoteCreateRequest;
import umc.catchy.domain.vote.dto.response.CategoryVoteResultResponse;
import umc.catchy.domain.vote.dto.response.GroupVoteResultResponse;
import umc.catchy.domain.vote.dto.response.PlaceVoteListResponse;
import umc.catchy.domain.vote.service.VoteService;
import umc.catchy.support.ControllerTestSupport;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VoteController.class)
class VoteControllerTest extends ControllerTestSupport {

    @MockitoBean
    private VoteService voteService;

    @Test
    @DisplayName("투표 생성 API 테스트")
    void createVote_api_test() throws Exception {
        // given
        VoteCreateRequest request = new VoteCreateRequest(1L);
        Vote mockVote = mock(Vote.class);
        when(mockVote.getId()).thenReturn(100L);
        when(voteService.createVote(any(VoteCreateRequest.class))).thenReturn(mockVote);

        // when & then
        mockMvc.perform(post("/vote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", testToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.voteId").value(100L));
    }

    @Test
    @DisplayName("카테고리 투표 제출 API 테스트")
    void submitVote_api_test() throws Exception {
        // given
        Long voteId = 100L;
        CategoryVoteRequest request = new CategoryVoteRequest(List.of(1L, 2L, 3L));

        doNothing().when(voteService).submitVote(anyLong(), anyList());

        // when & then
        mockMvc.perform(post("/vote/{voteId}/category", voteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("성공입니다."));
    }

    @Test
    @DisplayName("투표 현황 조회 API 테스트")
    void getVoteResults_api_test() throws Exception {
        // given
        Long voteId = 100L;

        CategoryVoteResultResponse.VotedMemberInfo memberInfo =
                CategoryVoteResultResponse.VotedMemberInfo.of(2L, "테스트유저", "https://image.url");

        CategoryVoteResultResponse.VoteResultInfo resultInfo =
                CategoryVoteResultResponse.VoteResultInfo.of("CAFE", 3, List.of(memberInfo), 1);

        CategoryVoteResultResponse response =
                CategoryVoteResultResponse.of("IN_PROGRESS", 5, List.of(resultInfo));

        when(voteService.getVoteResults(voteId)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/vote/{voteId}", voteId)
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.result.totalMembers").value(5))
                .andExpect(jsonPath("$.result.results[0].category").value("CAFE"))
                .andExpect(jsonPath("$.result.results[0].voteCount").value(3))
                .andExpect(jsonPath("$.result.results[0].rank").value(1))
                .andExpect(jsonPath("$.result.results[0].votedMembers[0].nickname").value("테스트유저"));
    }

    @Test
    @DisplayName("최종 카테고리 결과 조회 API 테스트")
    void getGroupVoteResults_api_test() throws Exception {
        // given
        Long groupId = 1L;
        Long voteId = 100L;

        GroupVoteResultResponse.CategoryResult categoryResult =
                new GroupVoteResultResponse.CategoryResult("CAFE", 5);

        GroupVoteResultResponse response = GroupVoteResultResponse.of(
                "서울시 강남구 테헤란로",
                List.of(categoryResult)
        );

        when(voteService.getGroupVoteResults(groupId, voteId)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/vote/{groupId}/votes/{voteId}/results", groupId, voteId)
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.groupLocation").value("서울시 강남구 테헤란로"))
                .andExpect(jsonPath("$.result.categories[0].category").value("CAFE"))
                .andExpect(jsonPath("$.result.categories[0].count").value(5));
    }

    @Test
    @DisplayName("카테고리 별 장소 목록 조회 API 테스트")
    void getPlacesByCategory_api_test() throws Exception {
        // given
        Long groupId = 1L;
        BigCategory category = BigCategory.CAFE;
        int pageSize = 10;

        CategoryVoteResultResponse.VotedMemberInfo memberInfo =
                CategoryVoteResultResponse.VotedMemberInfo.of(2L, "테스트유저", "https://image.url");

        PlaceVoteListResponse.PlaceVoteInfo placeInfo = PlaceVoteListResponse.PlaceVoteInfo.of(
                50L,
                "테스트 카페",
                "서울시 강남구 테헤란로 123",
                4.5,
                120L,
                "https://place-image.url",
                List.of(memberInfo)
        );

        PlaceVoteListResponse response = PlaceVoteListResponse.of(
                "서울시 강남구 테헤란로",
                List.of(placeInfo),
                true
        );

        when(voteService.getPlacesByCategory(eq(groupId), anyString(), eq(pageSize), any()))
                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/vote/{groupId}/categories/{category}/places", groupId, category)
                        .param("pageSize", String.valueOf(pageSize))
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.groupLocation").value("서울시 강남구 테헤란로"))
                .andExpect(jsonPath("$.result.places[0].placeName").value("테스트 카페"))
                .andExpect(jsonPath("$.result.places[0].roadAddress").value("서울시 강남구 테헤란로 123"))
                .andExpect(jsonPath("$.result.places[0].reviewCount").value(120))
                .andExpect(jsonPath("$.result.places[0].votedMembers[0].nickname").value("테스트유저"))
                .andExpect(jsonPath("$.result.isLast").value(true));
    }

    @Test
    @DisplayName("장소 투표 토글 API 테스트")
    void togglePlaceVote_api_test() throws Exception {
        // given
        Long groupId = 1L;
        Long voteId = 100L;
        PlaceVoteRequest request = new PlaceVoteRequest(50L);
        String successMessage = "Vote added successfully.";

        when(voteService.togglePlaceVote(eq(voteId), eq(groupId), any(PlaceVoteRequest.class)))
                .thenReturn(successMessage);

        // when & then
        mockMvc.perform(patch("/vote/{groupId}/{voteId}/places/vote", groupId, voteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result").value(successMessage));
    }
}
