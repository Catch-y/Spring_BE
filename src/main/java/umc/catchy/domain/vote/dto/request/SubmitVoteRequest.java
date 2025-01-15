package umc.catchy.domain.vote.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SubmitVoteRequest {
    @NotEmpty(message = "카테고리를 선택해야 합니다.")
    private List<Long> categoryIds;
}