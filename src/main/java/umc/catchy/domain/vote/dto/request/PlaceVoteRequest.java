package umc.catchy.domain.vote.dto.request;

import jakarta.validation.constraints.NotNull;

public record PlaceVoteRequest(
        @NotNull
        Long placeId
) {
}
