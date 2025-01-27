package umc.catchy.domain.place.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SetCategoryRequest {
    @NotBlank
    private String bigCategory;

    @NotBlank
    private String smallCategory;
}
