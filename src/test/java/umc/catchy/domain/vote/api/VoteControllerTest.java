package umc.catchy.domain.vote.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.vote.dto.request.CategoryVoteRequest;
import umc.catchy.domain.vote.dto.request.PlaceVoteRequest;
import umc.catchy.domain.vote.dto.request.VoteCreateRequest;
import umc.catchy.domain.vote.dto.response.CategoryVoteResultResponse;
import umc.catchy.domain.vote.dto.response.GroupVoteResultResponse;
import umc.catchy.domain.vote.dto.response.PlaceVoteListResponse;
import umc.catchy.domain.vote.service.VoteCommandService;
import umc.catchy.domain.vote.service.VoteFacade;
import umc.catchy.domain.vote.service.VoteQueryService;
import umc.catchy.support.ControllerTestSupport;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VoteController.class)
class VoteControllerTest extends ControllerTestSupport {

    @MockitoBean
    private VoteFacade voteFacade;

    @MockitoBean
    private VoteQueryService voteQueryService;

    @MockitoBean
    private VoteCommandService voteCommandService;

    @Test
    @DisplayName("투표 생성 API 테스트")
    void createVote_api_test() throws Exception {
        VoteCreateRequest request = new VoteCreateRequest(1L);
        when(voteFacade.createVote(any(VoteCreateRequest.class))).thenReturn(100L);

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
        Long voteId = 100L;
        CategoryVoteRequest request = new CategoryVoteRequest(List.of(1L, 2L, 3L));

        doNothing().when(voteFacade).submitVote(anyLong(), any(CategoryVoteRequest.class), anyLong());

        mockMvc.perform(post("/vote/{voteId}/category", voteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));
    }

    @Test
    @DisplayName("투표 현황 조회 API 테스트")
    void getVoteResults_api_test() throws Exception {
        Long voteId = 100L;
        CategoryVoteResultResponse.VotedMemberInfo memberInfo =
                CategoryVoteResultResponse.VotedMemberInfo.of(2L, "테스트유저", "https://image.url");
        CategoryVoteResultResponse.VoteResultInfo resultInfo =
                CategoryVoteResultResponse.VoteResultInfo.of("CAFE", 3, List.of(memberInfo), 1);
        CategoryVoteResultResponse response =
                CategoryVoteResultResponse.of("IN_PROGRESS", 5, List.of(resultInfo));

        when(voteQueryService.getVoteResults(voteId)).thenReturn(response);

        mockMvc.perform(get("/vote/{voteId}", voteId)
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.results[0].category").value("CAFE"));
    }

    @Test
    @DisplayName("최종 카테고리 결과 조회 API 테스트")
    void getGroupVoteResults_api_test() throws Exception {
        Long groupId = 1L;
        Long voteId = 100L;
        GroupVoteResultResponse response = GroupVoteResultResponse.of(
                "서울시 강남구 테헤란로",
                List.of(GroupVoteResultResponse.CategoryResult.of("CAFE", 5))
        );

        when(voteQueryService.getGroupVoteResults(groupId, voteId)).thenReturn(response);

        mockMvc.perform(get("/vote/{groupId}/votes/{voteId}/results", groupId, voteId)
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.groupLocation").value("서울시 강남구 테헤란로"));
    }

    @Test
    @DisplayName("카테고리 별 장소 목록 조회 API 테스트")
    void getPlacesByCategory_api_test() throws Exception {
        Long groupId = 1L;
        BigCategory category = BigCategory.CAFE;
        PlaceVoteListResponse response = PlaceVoteListResponse.of("서울시 강남구", List.of(), true);

        when(voteQueryService.getPlacesByCategory(eq(groupId), eq(category), anyInt(), any()))
                .thenReturn(response);

        mockMvc.perform(get("/vote/{groupId}/categories/{category}/places", groupId, category)
                        .param("pageSize", "10")
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));
    }

    @Test
    @DisplayName("장소 투표 토글 API 테스트")
    void togglePlaceVote_api_test() throws Exception {
        Long groupId = 1L;
        Long voteId = 100L;
        PlaceVoteRequest request = new PlaceVoteRequest(50L);
        when(voteCommandService.togglePlaceVote(eq(voteId), eq(groupId), any(PlaceVoteRequest.class), anyLong()))
                .thenReturn("Vote added successfully.");

        mockMvc.perform(patch("/vote/{groupId}/{voteId}/places/vote", groupId, voteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("Vote added successfully."));
    }
}
