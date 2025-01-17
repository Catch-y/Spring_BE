package umc.catchy.domain.course.dto.request;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class CourseUpdateRequest {
    private String courseName;
    private String courseDescription;
    private List<Long> placeIds;
    private MultipartFile profileImage;
}
