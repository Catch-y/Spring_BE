package umc.catchy.domain.reviewReport.api;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import umc.catchy.domain.reviewReport.domain.ReviewType;
import umc.catchy.domain.reviewReport.dto.request.PostReviewReportRequest;
import umc.catchy.domain.reviewReport.dto.response.DeleteReviewResponse;
import umc.catchy.domain.reviewReport.dto.response.MyPageReviewsResponse;
import umc.catchy.domain.reviewReport.dto.response.PostReviewReportResponse;
import umc.catchy.domain.reviewReport.service.ReviewReportService;
import umc.catchy.global.common.response.BaseResponse;
import umc.catchy.global.common.response.status.SuccessStatus;

import java.time.LocalDate;

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

    @Operation(summary = "리뷰 삭제 API", description = "코스리뷰, 장소리뷰 통합 리뷰 삭제 API입니다.")
    @DeleteMapping("/mypage/reviews/{reviewId}")
    public ResponseEntity<BaseResponse<DeleteReviewResponse>> deleteReview(
            @PathVariable Long reviewId,
            @RequestParam String reviewType
    ){
        DeleteReviewResponse response = reviewReportService.deleteReview(reviewId, reviewType);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, response));
    }

    @Operation(summary = "마이페이지/내 장소 리뷰 조회 API", description = "내가 작성한 장소 리뷰를 조회하는 API입니다.")
    @GetMapping("mypage/placeReviews")
    public ResponseEntity<BaseResponse<MyPageReviewsResponse.ReviewsDTO>> getMyPlaceReviews(
            @RequestParam int pageSize,
            @RequestParam(required = false) LocalDate lastPlaceReviewDate,
            @RequestParam(required = false) Long lastReviewId
    ){
        MyPageReviewsResponse.ReviewsDTO response = reviewReportService.getMyReviews("PLACE", pageSize, lastPlaceReviewDate, lastReviewId);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, response));
    }

    @Operation(summary = "마이페이지/내 코스 리뷰 조회 API", description = "내가 작성한 코스 리뷰를 조회하는 API입니다.")
    @GetMapping("mypage/courseReviews")
    public ResponseEntity<BaseResponse<MyPageReviewsResponse.ReviewsDTO>> getMyCourseReviews(
            @RequestParam int pageSize,
            @RequestParam(required = false) Long lastReviewId
    ){
        MyPageReviewsResponse.ReviewsDTO response = reviewReportService.getMyReviews("COURSE", pageSize, null, lastReviewId);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, response));
    }
}
