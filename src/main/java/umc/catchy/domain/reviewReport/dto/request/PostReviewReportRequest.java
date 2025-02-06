package umc.catchy.domain.reviewReport.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PostReviewReportRequest {
    @NotBlank
    @Pattern(regexp = "COURSE|PLACE", message = "리뷰 타입은 COURSE 또는 PLACE입니다.")
    private String reviewType;
    @NotBlank(message = "신고 이유를 적어주세요.")
    private String reason;
}
