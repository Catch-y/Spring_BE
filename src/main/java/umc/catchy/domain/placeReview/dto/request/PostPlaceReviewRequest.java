package umc.catchy.domain.placeReview.dto.request;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

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

    @Size(max = 5, message = "이미지는 최대 5개까지만 업로드 가능합니다.")
    List<MultipartFile> images;
}
