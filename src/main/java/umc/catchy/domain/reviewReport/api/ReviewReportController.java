package umc.catchy.domain.reviewReport.api;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import umc.catchy.domain.reviewReport.dto.request.PostReviewReportRequest;
import umc.catchy.domain.reviewReport.dto.response.DeleteReviewResponse;
import umc.catchy.domain.reviewReport.dto.response.PostReviewReportResponse;
import umc.catchy.domain.reviewReport.service.ReviewReportService;
import umc.catchy.global.common.response.BaseResponse;
import umc.catchy.global.common.response.status.SuccessStatus;

@RestController
@RequiredArgsConstructor
public class ReviewReportController {

    private final ReviewReportService reviewReportService;

    @Operation(summary = "리뷰 신고 API", description = "코스 리뷰, 장소 리뷰 통합 리뷰 신고 API입니다.")
    @PostMapping("/reviews/{reviewId}/report")
    public ResponseEntity<BaseResponse<PostReviewReportResponse>> postReviewReport(
            @PathVariable Long reviewId,
            @Valid @RequestBody PostReviewReportRequest request
            ){
        PostReviewReportResponse response = reviewReportService.postReviewReport(reviewId, request);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, response));
    }

    //TODO 리뷰 삭제하기
    @Operation(summary = "리뷰 삭제 API", description = "코스리뷰, 장소리뷰 통합 리뷰 삭제 API입니다.")
    @DeleteMapping("/mypage/reviews/{reviewId}")
    public ResponseEntity<BaseResponse<DeleteReviewResponse>> deleteReview(
            @PathVariable Long reviewId,
            @RequestParam String reviewType
    ){
        DeleteReviewResponse response = reviewReportService.deleteReview(reviewId, reviewType);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, response));
    }
}
