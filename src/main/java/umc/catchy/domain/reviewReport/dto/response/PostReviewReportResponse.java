package umc.catchy.domain.reviewReport.dto.response;

import umc.catchy.domain.reviewReport.domain.ReviewReport;
import umc.catchy.domain.reviewReport.domain.ReviewType;

public record PostReviewReportResponse(
        Long reportId,
        ReviewType reviewType,
        String message
) {
    public static PostReviewReportResponse from(ReviewReport reviewReport) {
        return new PostReviewReportResponse(
                reviewReport.getId(),
                reviewReport.getReviewType(),
                "해당 리뷰가 성공적으로 신고되었습니다."
        );
    }
}
