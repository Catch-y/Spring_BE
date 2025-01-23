package umc.catchy.domain.course.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GptCourseInfoResponse {
    private String courseName;
    private String courseDescription;
    private String recommendTime; // "HH:mm~HH:mm" 형식
    private String courseImage;
    private List<GptPlaceInfoResponse> placeInfos;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GptPlaceInfoResponse {
        private Long placeId;
        private String name;
        private String roadAddress;
        private String operatingHours; // "HH:mm-HH:mm" 형식
        private String recommendVisitTime; // "HH:mm~HH:mm" 형식
    }
}