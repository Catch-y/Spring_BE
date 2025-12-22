package umc.catchy.domain.reviewReport.dto.response;

import umc.catchy.domain.reviewReport.domain.ReviewType;
import umc.catchy.domain.reviewReport.dto.query.CourseReviewDto;
import umc.catchy.domain.reviewReport.dto.query.PlaceReviewDto;
import umc.catchy.domain.reviewReport.dto.query.ReviewImageDto;

import java.util.List;

public record MyPageReviewsResponse(
        ReviewType reviewType,
        Integer reviewCount,
        List<?> content,
        Boolean last
) {
    public static MyPageReviewsResponse of(ReviewType reviewType, Integer reviewCount, List<?> content, Boolean last) {
        return new MyPageReviewsResponse(reviewType, reviewCount, content, last);
    }

    public record CourseReviewContent(
            Long reviewId, String name, String comment,
            List<ReviewImage> reviewImages, String courseType, List<String> categories
    ) {
        public static CourseReviewContent from(CourseReviewDto dto) {
            return new CourseReviewContent(
                    dto.getReviewId(),
                    dto.getName(),
                    dto.getComment(),
                    dto.getReviewImages().stream().map(ReviewImage::from).toList(),
                    dto.getCourseType().name(),
                    dto.getCategories().stream().map(Enum::name).distinct().toList()
            );
        }
    }

    public record PlaceReviewContent(
            Long reviewId, String name, String comment,
            List<ReviewImage> reviewImages, String category, Integer rating, String visitedDate
    ) {
        public static PlaceReviewContent from(PlaceReviewDto dto) {
            return new PlaceReviewContent(
                    dto.getReviewId(),
                    dto.getName(),
                    dto.getComment(),
                    dto.getReviewImages().stream().map(ReviewImage::from).toList(),
                    dto.getCategory().name(),
                    dto.getRating(),
                    dto.getVisitedDate().toString()
            );
        }
    }

    public record ReviewImage(Long reviewImageId, String imageUrl) {
        public static ReviewImage from(ReviewImageDto dto) {
            return new ReviewImage(dto.getReviewImageId(), dto.getImageUrl());
        }
    }
}
