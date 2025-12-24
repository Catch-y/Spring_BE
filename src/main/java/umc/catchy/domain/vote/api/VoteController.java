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
import umc.catchy.domain.vote.dto.request.CategoryVoteRequest;
import umc.catchy.domain.vote.dto.request.PlaceVoteRequest;
import umc.catchy.domain.vote.dto.request.VoteCreateRequest;
import umc.catchy.domain.vote.dto.response.*;
import umc.catchy.domain.vote.service.VoteCommandService;
import umc.catchy.domain.vote.service.VoteFacade;
import umc.catchy.domain.vote.service.VoteQueryService;
import umc.catchy.global.common.response.BaseResponse;
import umc.catchy.global.common.response.status.SuccessStatus;
import umc.catchy.global.util.SecurityUtil;

@Tag(name = "Vote", description = "투표 관련 API")
@RestController
@RequestMapping("/vote")
@RequiredArgsConstructor
public class VoteController {

    private final VoteFacade voteFacade;
    private final VoteQueryService voteQueryService;
    private final VoteCommandService voteCommandService;

    @Operation(summary = "투표 생성", description = "새로운 투표를 생성합니다.")
    @PostMapping
    public ResponseEntity<BaseResponse<VoteIdResponse>> createVote(@Valid @RequestBody VoteCreateRequest request) {
        Long voteId = voteFacade.createVote(request);
        return ResponseEntity.status(SuccessStatus._CREATED.getHttpStatus())
                .body(BaseResponse.onSuccess(SuccessStatus._CREATED, VoteIdResponse.of(voteId)));
    }

    @Operation(summary = "카테고리 투표", description = "최소 2개 이상 카테고리를 투표합니다.")
    @PostMapping("/{voteId}/category")
    public ResponseEntity<BaseResponse<Void>> submitVote(
            @PathVariable Long voteId,
            @Valid @RequestBody CategoryVoteRequest request) {
        voteFacade.submitVote(voteId, request, SecurityUtil.getCurrentMemberId());
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, null));
    }

    @Operation(summary = "투표 진행 중", description = "카테고리 투표 진행 중 투표 현황 조회")
    @GetMapping("/{voteId}")
    public ResponseEntity<BaseResponse<CategoryVoteResultResponse>> getVoteResults(@PathVariable Long voteId) {
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, voteQueryService.getVoteResults(voteId)));
    }

    @Operation(summary = "투표 진행 중", description = "카테고리 투표 진행 중 멤버 별 투표 현황 조회")
    @GetMapping("/{groupId}/votes/{voteId}/members")
    public ResponseEntity<BaseResponse<MemberVoteStatusResponse>> getGroupVoteStatus(
            @PathVariable Long groupId, @PathVariable Long voteId) {
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, voteQueryService.getGroupVoteStatus(groupId, voteId)));
    }

    @Operation(summary = "투표 진행 중", description = "해당 투표의 카테고리 ID 목록 조회")
    @GetMapping("/{voteId}/category")
    public ResponseEntity<BaseResponse<CategoryVoteListResponse>> getCategories(@PathVariable Long voteId) {
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, voteQueryService.getCategoriesByVoteId(voteId)));
    }

    @Operation(summary = "투표 완료 - 카테고리 확인", description = "특정 투표 결과의 카테고리를 조회합니다.")
    @GetMapping("/{groupId}/votes/{voteId}/results")
    public ResponseEntity<BaseResponse<GroupVoteResultResponse>> getGroupVoteResults(
            @PathVariable Long groupId, @PathVariable Long voteId) {
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, voteQueryService.getGroupVoteResults(groupId, voteId)));
    }

    @Operation(summary = "투표 완료 - 카테고리 별 장소 확인", description = "선정된 카테고리 별 장소를 조회합니다.")
    @GetMapping("/{groupId}/categories/{category}/places")
    public ResponseEntity<BaseResponse<PlaceVoteListResponse>> getPlacesByCategory(
            @PathVariable Long groupId,
            @Parameter(description = "카테고리 값", schema = @Schema(implementation = BigCategory.class)) @PathVariable BigCategory category,
            @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) Long lastPlaceId) {
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, voteQueryService.getPlacesByCategory(groupId, category, pageSize, lastPlaceId)));
    }

    @PatchMapping("/{groupId}/{voteId}/places/vote")
    @Operation(summary = "장소 투표/취소", description = "좋아요와 같은 방식으로 장소 투표를 토글합니다.")
    public ResponseEntity<BaseResponse<String>> togglePlaceVote(
            @PathVariable Long groupId, @PathVariable Long voteId, @Validated @RequestBody PlaceVoteRequest request) {
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, voteCommandService.togglePlaceVote(voteId, groupId, request, SecurityUtil.getCurrentMemberId())));
    }

    @Operation(summary = "카테고리 재투표", description = "기존 투표를 취소하고 다시 투표합니다.")
    @PostMapping("/{voteId}/category/revote")
    public ResponseEntity<BaseResponse<Void>> revoteCategory(
            @PathVariable Long voteId, @Valid @RequestBody CategoryVoteRequest request) {
        voteFacade.revoteCategory(voteId, request, SecurityUtil.getCurrentMemberId());
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, null));
    }
}
