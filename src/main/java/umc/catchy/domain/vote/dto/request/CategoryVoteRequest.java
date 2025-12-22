package umc.catchy.domain.vote.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CategoryVoteRequest(
        @NotEmpty(message = "카테고리를 선택해야 합니다.")
        List<Long> categoryIds
) {
}
