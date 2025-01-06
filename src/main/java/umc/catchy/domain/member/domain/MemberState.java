package umc.catchy.domain.member.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MemberState {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE");

    private final String value;
}
