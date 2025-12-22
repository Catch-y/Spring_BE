package umc.catchy.domain.courseReview.dto.response;

import umc.catchy.domain.courseReviewImage.domain.CourseReviewImage;

public record CourseReviewImageResponse(
        Long reviewImageId,
        String imageUrl
) {
    public static CourseReviewImageResponse from(CourseReviewImage image) {
        return new CourseReviewImageResponse(
                image.getId(),
                image.getImageUrl()
        );
    }
}
