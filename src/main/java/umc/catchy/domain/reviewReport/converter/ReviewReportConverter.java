package umc.catchy.domain.reviewReport.converter;

import umc.catchy.domain.courseReview.domain.CourseReview;
import umc.catchy.domain.placeReview.domain.PlaceReview;
import umc.catchy.domain.reviewReport.domain.ReviewReport;
import umc.catchy.domain.reviewReport.domain.ReviewType;
import umc.catchy.domain.reviewReport.dto.request.PostReviewReportRequest;
import umc.catchy.domain.reviewReport.dto.response.DeleteReviewResponse;
import umc.catchy.domain.reviewReport.dto.response.PostReviewReportResponse;

public class ReviewReportConverter {

    public static DeleteReviewResponse toDeleteReviewResponse(Long reviewId, ReviewType reviewType){
        return DeleteReviewResponse.builder()
                .reviewId(reviewId)
                .reviewType(reviewType)
                .message("해당 리뷰가 성공적으로 삭제되었습니다.")
                .build();
    }

    public static PostReviewReportResponse toPostReviewReportResponse(ReviewReport reviewReport) {
        return PostReviewReportResponse.builder()
                .reportId(reviewReport.getId())
                .reviewType(reviewReport.getReviewType())
                .message("해당 리뷰가 성공적으로 신고되었습니다.")
                .build();
    }

    public static ReviewReport toCourseReviewReport(PostReviewReportRequest request, CourseReview courseReview) {
        return ReviewReport.builder()
                .reviewType(ReviewType.valueOf(request.getReviewType()))
                .reason(request.getReason())
                .courseReview(courseReview)
                .build();
    }

    public static ReviewReport toPlaceReviewReport(PostReviewReportRequest request, PlaceReview placeReview){
        return ReviewReport.builder()
                .reviewType(ReviewType.valueOf(request.getReviewType()))
                .reason(request.getReason())
                .placeReview(placeReview)
                .build();
    }
}
