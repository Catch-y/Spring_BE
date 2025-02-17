package umc.catchy.domain.reviewReport.dto.response;

import lombok.*;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.course.domain.CourseType;
import umc.catchy.domain.reviewReport.domain.ReviewType;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public class MyPageReviewsResponse {

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ReviewImagesDTO{
        Long reviewImageId;
        String imageUrl;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BaseReviewDTO{
        Long reviewId;
        String name;    //장소이름 또는 코스이름
        String comment;
        List<ReviewImagesDTO> reviewImages;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PlaceReviewDTO extends BaseReviewDTO {
        BigCategory category;
        Integer rating;
        LocalDate visitedDate;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CourseReviewDTO extends BaseReviewDTO {
        CourseType courseType;
        List<BigCategory> categories;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ReviewsDTO{
        ReviewType reviewType;
        Integer reviewCount;
        List<? extends BaseReviewDTO> content;
        Boolean last;
    }
}
