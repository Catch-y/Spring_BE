package umc.catchy.domain.course.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PopularCourseInfoResponse {
    private Long courseId;
    private String courseImage;
    private String courseName;
}
