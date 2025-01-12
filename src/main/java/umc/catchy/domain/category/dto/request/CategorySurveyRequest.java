package umc.catchy.domain.category.dto.request;


import lombok.Getter;

import java.util.List;

@Getter
public class CategorySurveyRequest {
    private List<String> categories;
}
