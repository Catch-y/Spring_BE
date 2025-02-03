package umc.catchy.domain.reviewReport.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import umc.catchy.domain.reviewReport.domain.ReviewType;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostReviewReportResponse {
    Long reportId;
    ReviewType reviewType;
    String message;
}
