package umc.catchy.domain.courseReview.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

public record PostCourseReviewRequest(
        @NotBlank
        @Size(max = 300, message = "최대 300자까지 입력 가능합니다.")
        String comment,

        @Size(max = 5, message = "이미지는 최대 5개까지만 업로드 가능합니다.")
        List<MultipartFile> images
) {
    public PostCourseReviewRequest {
        if (images == null) {
            images = Collections.emptyList();
        }
    }
}
