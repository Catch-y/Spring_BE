package umc.catchy.domain.mapping.memberCategory.dto.response;

import java.util.List;

public record MemberCategoryCreatedResponse(
        List<Long> memberCategoryIds
) {
}