package umc.catchy.domain.vote.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryResult {
    private String category;
    private int count;

    public CategoryResult(String category, int count) {
        this.category = category;
        this.count = count;
    }
}
