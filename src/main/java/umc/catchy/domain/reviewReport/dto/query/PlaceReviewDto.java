package umc.catchy.domain.reviewReport.dto.query;

import lombok.Getter;
import umc.catchy.domain.category.domain.BigCategory;

import java.time.LocalDate;
import java.util.List;

@Getter
public class PlaceReviewDto {
    private final Long reviewId;
    private final String name;
    private final String comment;
    private final List<ReviewImageDto> reviewImages;
    private final BigCategory category;
    private final Integer rating;
    private final LocalDate visitedDate;

    public PlaceReviewDto(Long reviewId, String name, String comment, List<ReviewImageDto> reviewImages,
                          BigCategory category, Integer rating, LocalDate visitedDate) {
        this.reviewId = reviewId;
        this.name = name;
        this.comment = comment;
        this.reviewImages = reviewImages;
        this.category = category;
        this.rating = rating != null ? rating : 0;
        this.visitedDate = visitedDate;
    }
}
