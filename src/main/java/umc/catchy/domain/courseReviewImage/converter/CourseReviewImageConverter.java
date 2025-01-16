package umc.catchy.domain.courseReviewImage.converter;

import umc.catchy.domain.courseReview.domain.CourseReview;
import umc.catchy.domain.courseReview.dto.response.PostCourseReviewResponse;
import umc.catchy.domain.courseReviewImage.domain.CourseReviewImage;
import umc.catchy.domain.placeReviewImage.domain.PlaceReviewImage;

public class CourseReviewImageConverter {

    public static PostCourseReviewResponse.courseReviewImageResponseDTO toCourseReviewImageResponseDTO(CourseReviewImage courseReviewImage) {
        return PostCourseReviewResponse.courseReviewImageResponseDTO.builder()
                .reviewImageId(courseReviewImage.getId())
                .imageUrl(courseReviewImage.getImageUrl())
                .build();
    }

    public static CourseReviewImage toCourseReviewImage(String url, CourseReview courseReview) {
        return CourseReviewImage.builder()
                .imageUrl(url)
                .courseReview(courseReview)
                .build();
    }
}
