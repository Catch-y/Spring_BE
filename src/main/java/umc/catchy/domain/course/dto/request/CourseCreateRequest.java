package umc.catchy.domain.course.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class CourseCreateRequest {
    @NotBlank(message = "코스 이름은 필수 입력 항목입니다.")
    private String courseName;

    @NotBlank(message = "코스 설명은 필수 입력 항목입니다.")
    private String courseDescription;

    @Size(min = 2, max = 5, message = "장소는 2개 이상 5개 이하로 선택되어야 합니다.")
    private List<Long> placeIds;

    private MultipartFile courseImage;

    @NotNull(message = "추천 시간대는 필수 입력 항목입니다.")
    @Schema(description = "HH:mm 형식으로 입력해주세요.", example = "10:00")
    private String recommendTimeStart;

    @NotNull(message = "추천 시간대는 필수 입력 항목입니다.")
    @Schema(description = "HH:mm 형식으로 입력해주세요.", example = "22:00")
    private String recommendTimeEnd;
}
