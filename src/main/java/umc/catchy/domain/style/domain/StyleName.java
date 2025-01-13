package umc.catchy.domain.style.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StyleName {
    ALONE("혼자"),
    FRIENDS("친구"),
    FAMILY("가족"),
    COUPLE("연인");

    private final String value;
}
