package umc.catchy.domain.vote.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record VoteCreateRequest(
        @Schema(description = "그룹 ID", example = "1")
        @NotNull
        Long groupId
) {
}
