package umc.catchy.domain.mapping.memberLocation.dto.response;

import java.util.List;

public record MemberLocationCreatedResponse(List<Long> memberLocationId) {
    public static MemberLocationCreatedResponse of(List<Long> memberLocationId) {
        return new MemberLocationCreatedResponse(memberLocationId);
    }
}
