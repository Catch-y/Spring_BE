package umc.catchy.domain.reviewReport.dto.response;

import umc.catchy.domain.reviewReport.domain.ReviewType;

public record DeleteReviewResponse(
        Long reviewId,
        ReviewType reviewType,
        String message
) {
    public static DeleteReviewResponse of(Long reviewId, ReviewType reviewType) {
        return new DeleteReviewResponse(
                reviewId,
                reviewType,
                "해당 리뷰가 성공적으로 삭제되었습니다."
        );
    }
}
