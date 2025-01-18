package umc.catchy.domain.course.dto.request;

import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class CourseUpdateRequest {
    private String courseName;

    private String courseDescription;

    @Size(min = 2, max = 5, message = "장소는 2개 이상 5개 이하로 선택되어야 합니다.")
    private List<Long> placeIds;

    private MultipartFile courseImage;

    private LocalTime recommendTimeStart;

    private LocalTime recommendTimeEnd;
}
