package umc.catchy.domain.vote.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlaceVoteRequest {
    @NotNull
    private Long placeId;
}