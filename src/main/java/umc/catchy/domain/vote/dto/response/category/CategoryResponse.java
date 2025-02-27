package umc.catchy.domain.vote.dto.response.category;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CategoryResponse {
    private Long voteId;
    private List<CategoryDto> categories;
}
