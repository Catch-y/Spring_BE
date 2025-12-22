package umc.catchy.domain.reviewReport.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PostReviewReportRequest(
        @NotBlank
        @Pattern(regexp = "COURSE|PLACE", message = "리뷰 타입은 COURSE 또는 PLACE입니다.")
        String reviewType,

        @NotBlank(message = "신고 이유를 적어주세요.")
        String reason
) {
}