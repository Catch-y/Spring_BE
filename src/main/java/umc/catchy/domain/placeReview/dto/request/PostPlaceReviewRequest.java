package umc.catchy.domain.placeReview.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PostPlaceReviewRequest {

    @NotNull(message = "1~5 중 평점을 골라주세요.")
    @Min(1)
    @Max(5)
    Integer rating;

    @NotBlank
    @Size(max = 300, message = "최대 300자까지 입력 가능합니다.")
    String comment;

    @NotNull(message = "방문날짜를 입력해주세요/YY-MM-DD")
    LocalDate visitedDate;

    @Size(max = 5, message = "이미지는 최대 5개까지만 업로드 가능합니다.")
    List<MultipartFile> images;
}
