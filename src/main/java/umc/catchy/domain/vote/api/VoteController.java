package umc.catchy.domain.vote.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import umc.catchy.domain.vote.dto.request.CreateVoteRequest;
import umc.catchy.domain.vote.dto.request.SubmitVoteRequest;
import umc.catchy.domain.vote.dto.response.GroupVoteStatusResponse;
import umc.catchy.domain.vote.dto.response.VoteResponse;
import umc.catchy.domain.vote.dto.response.VoteResultResponse;
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
            @RequestBody SubmitVoteRequest request,
            @AuthenticationPrincipal Long memberId) {
        voteService.submitVote(memberId, voteId, request.getCategoryIds());
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
}
