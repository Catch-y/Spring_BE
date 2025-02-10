package umc.catchy.domain.course.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import umc.catchy.domain.category.domain.BigCategory;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GptCourseInfoResponse {
    private Long courseId;
    private String courseName;
    private String courseDescription;
    private String recommendTime; // "HH:mm~HH:mm" 형식
    private String courseImage;
    private Double courseRating;
    private List<GptPlaceInfoResponse> placeInfos;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class GptPlaceInfoResponse {
        private Long placeId;
        private String placeName;
        private String placeImage;
        private String category;
        private String roadAddress;
        private String activeTime;
        private double rating;
        private int reviewCount;

        // QueryDSL을 위한 생성자
        public GptPlaceInfoResponse(Long placeId, String placeName, String placeImage, String categoryKey,
                                    String roadAddress, String activeTime, double rating, int reviewCount) {
            this.placeId = placeId;
            this.placeName = placeName;
            this.placeImage = placeImage;
            this.category = BigCategory.valueOf(categoryKey).getValue();
            this.roadAddress = roadAddress;
            this.activeTime = activeTime;
            this.rating = rating;
            this.reviewCount = reviewCount;
        }
    }
}