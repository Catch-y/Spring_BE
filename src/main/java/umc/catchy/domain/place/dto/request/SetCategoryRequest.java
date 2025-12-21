package umc.catchy.domain.place.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SetCategoryRequest(
        @NotBlank(message = "대카테고리는 필수 입력 항목입니다.")
        String bigCategory,

        @NotBlank(message = "소카테고리는 필수 입력 항목입니다.")
        String smallCategory
) {
}