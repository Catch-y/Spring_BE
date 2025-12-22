package umc.catchy.domain.courseReview.dto.query;

import lombok.Getter;
import java.time.LocalDate;

@Getter
public class CourseReviewDto {
    private final Long reviewId;
    private final String comment;
    private final LocalDate createdAt;
    private final String creatorNickname;

    public CourseReviewDto(Long reviewId, String comment, LocalDate createdAt, String creatorNickname) {
        this.reviewId = reviewId;
        this.comment = comment;
        this.createdAt = createdAt;
        this.creatorNickname = creatorNickname;
    }
}
