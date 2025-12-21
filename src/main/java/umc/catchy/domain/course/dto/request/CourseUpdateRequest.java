package umc.catchy.domain.course.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record CourseUpdateRequest(
        String courseName,

        String courseDescription,

        @Size(min = 2, max = 5, message = "장소는 2개 이상 5개 이하로 선택되어야 합니다.")
        List<Long> placeIds,

        MultipartFile courseImage,

        @Schema(description = "HH:mm 형식으로 입력해주세요.", example = "10:00")
        String recommendTimeStart,

        @Schema(description = "HH:mm 형식으로 입력해주세요.", example = "22:00")
        String recommendTimeEnd
) {
}
