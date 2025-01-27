package umc.catchy.domain.mapping.memberCategory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MemberCategoryCreatedResponse {
    List<Long> memberCategoryIds;
}
