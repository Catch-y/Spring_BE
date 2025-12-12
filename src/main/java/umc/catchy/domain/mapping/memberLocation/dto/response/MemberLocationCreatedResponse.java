package umc.catchy.domain.mapping.memberLocation.dto.response;

import java.util.List;

public record MemberLocationCreatedResponse(
        List<Long> memberLocationId
) {
}