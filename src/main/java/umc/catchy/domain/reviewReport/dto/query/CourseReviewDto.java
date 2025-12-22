package umc.catchy.domain.reviewReport.dto.query;

import lombok.Getter;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.course.domain.CourseType;

import java.util.List;

@Getter
public class CourseReviewDto {
    private final Long reviewId;
    private final String name;
    private final String comment;
    private final List<ReviewImageDto> reviewImages;
    private final CourseType courseType;
    private final List<BigCategory> categories;

    public CourseReviewDto(Long reviewId, String name, String comment, List<ReviewImageDto> reviewImages,
                           CourseType courseType, List<BigCategory> categories) {
        this.reviewId = reviewId;
        this.name = name;
        this.comment = comment;
        this.reviewImages = reviewImages;
        this.courseType = courseType;
        this.categories = categories;
    }
}
