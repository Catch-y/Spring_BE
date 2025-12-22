package umc.catchy.domain.reviewReport.dto.query;

import lombok.Getter;

@Getter
public class ReviewImageDto {
    private final Long reviewImageId;
    private final String imageUrl;

    public ReviewImageDto(Long reviewImageId, String imageUrl) {
        this.reviewImageId = reviewImageId;
        this.imageUrl = imageUrl;
    }
}
