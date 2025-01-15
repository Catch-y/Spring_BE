package umc.catchy.domain.vote.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import umc.catchy.domain.vote.dto.request.CreateVoteRequest;
import umc.catchy.domain.vote.dto.response.VoteResponse;
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
}
