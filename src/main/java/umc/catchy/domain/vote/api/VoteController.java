package umc.catchy.domain.vote.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.vote.dto.request.CreateVoteRequest;
import umc.catchy.domain.vote.dto.request.PlaceVoteRequest;
import umc.catchy.domain.vote.dto.request.SubmitVoteRequest;
import umc.catchy.domain.vote.dto.response.category.CategoryResponse;
import umc.catchy.domain.vote.dto.response.group.GroupPlaceResponse;
import umc.catchy.domain.vote.dto.response.group.GroupVoteResultResponse;
import umc.catchy.domain.vote.dto.response.group.GroupVoteStatusResponse;
import umc.catchy.domain.vote.dto.response.vote.VoteResponse;
import umc.catchy.domain.vote.dto.response.vote.VoteResultResponse;
import umc.catchy.domain.vote.service.VoteService;
import umc.catchy.global.common.response.BaseResponse;
import umc.catchy.global.common.response.status.SuccessStatus;

@Tag(name = "Vote", description = "투표 관련 API")
@RestController
@RequestMapping("/vote")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @Operation(summary = "투표 생성", description = "새로운 투표를 생성합니다.")
    @PostMapping
    public ResponseEntity<BaseResponse<VoteResponse>> createVote(@Valid @RequestBody CreateVoteRequest request) {
        Long voteId = voteService.createVote(request).getId();
        VoteResponse response = VoteResponse.builder()
                .voteId(voteId)
                .build();
        return ResponseEntity.status(SuccessStatus._CREATED.getHttpStatus())
                .body(BaseResponse.onSuccess(SuccessStatus._CREATED, response));
    }

    @Operation(summary = "카테고리 투표", description = "최소 2개 이상 카테고리를 투표합니다.")
    @PostMapping("/{voteId}/category")
    public ResponseEntity<BaseResponse<Void>> submitVote(
            @PathVariable Long voteId,
            @RequestBody SubmitVoteRequest request) {
        voteService.submitVote(voteId, request.getCategoryIds());
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, null));
    }

    @Operation(summary = "투표 진행 중", description = "카테고리 투표 진행 중 투표 현황 조회")
    @GetMapping("/{voteId}")
    public ResponseEntity<BaseResponse<VoteResultResponse>> getVoteResults(@PathVariable Long voteId) {
        VoteResultResponse response = voteService.getVoteResults(voteId);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, response));
    }

    @Operation(summary = "투표 진행 중", description = "카테고리 투표 진행 중 멤버 별 투표 현황 조회")
    @GetMapping("/{groupId}/votes/{voteId}/members")
    public ResponseEntity<BaseResponse<GroupVoteStatusResponse>> getGroupVoteStatus(
            @PathVariable Long groupId,
            @PathVariable Long voteId
    ) {
        GroupVoteStatusResponse response = voteService.getGroupVoteStatus(groupId, voteId);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, response));
    }

    @Operation(summary = "투표 진행 중", description = "해당 카테고리 ID 목록 조회")
    @GetMapping("/{voteId}/category")
    public ResponseEntity<BaseResponse<CategoryResponse>> getCategories(
            @PathVariable Long voteId) {
        CategoryResponse response = voteService.getCategoriesByVoteId(voteId);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, response));
    }

    @Operation(summary = "투표 완료 - 카테고리 확인", description = "특정 투표 결과의 카테고리를 조회합니다.")
    @GetMapping("/{groupId}/votes/{voteId}/results")
    public ResponseEntity<BaseResponse<GroupVoteResultResponse>> getGroupVoteResults(
            @PathVariable Long groupId,
            @PathVariable Long voteId) {
        GroupVoteResultResponse response = voteService.getGroupVoteResults(groupId, voteId);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, response));
    }

    @Operation(summary = "투표 완료 - 카테고리 별 장소 확인", description = "카테고리 별 장소를 조회합니다.")
    @GetMapping("/{groupId}/categories/{category}/places")
    public ResponseEntity<BaseResponse<GroupPlaceResponse>> getPlacesByCategory(
            @PathVariable Long groupId,
            @Parameter(description = "카테고리 값", schema = @Schema(implementation = BigCategory.class))
            @PathVariable BigCategory category,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long lastPlaceId
    ) {
        GroupPlaceResponse response = voteService.getPlacesByCategory(groupId, category.name(), pageSize, lastPlaceId);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, response));
    }

    @PatchMapping("/{groupId}/{voteId}/places/vote")
    @Operation(summary = "장소 투표/취소", description = "좋아요와 같은 방식으로 장소 투표를 토글합니다.")
    public ResponseEntity<BaseResponse<String>> togglePlaceVote(
            @PathVariable Long groupId,
            @PathVariable Long voteId,
            @Validated @RequestBody PlaceVoteRequest request) {
        String message = voteService.togglePlaceVote(voteId, groupId, request);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, message));
    }

    @Operation(summary = "카테고리 재투표", description = "기존 투표를 취소하고 다시 투표합니다.")
    @PostMapping("/{voteId}/category/revote")
    public ResponseEntity<BaseResponse<Void>> revoteCategory(
            @PathVariable Long voteId,
            @RequestBody SubmitVoteRequest request) {
        voteService.revoteCategory(voteId, request.getCategoryIds());
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, null));
    }
}
