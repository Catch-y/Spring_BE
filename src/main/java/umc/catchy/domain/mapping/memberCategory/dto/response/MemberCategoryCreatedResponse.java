package umc.catchy.domain.mapping.memberCategory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class MemberCategoryCreatedResponse {
    private boolean success;
    private String message;
}
