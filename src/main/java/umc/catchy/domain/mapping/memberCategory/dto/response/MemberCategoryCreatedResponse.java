package umc.catchy.domain.mapping.memberCategory.dto.response;

import java.util.List;

public record MemberCategoryCreatedResponse(List<Long> memberCategoryIds) {
    public static MemberCategoryCreatedResponse of(List<Long> memberCategoryIds) {
        return new MemberCategoryCreatedResponse(memberCategoryIds);
    }
}
