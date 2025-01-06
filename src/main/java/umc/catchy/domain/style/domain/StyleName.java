package umc.catchy.domain.style.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StyleName {
    ALONE("ALONE"),
    FRIENDS("FRIENDS"),
    FAMILY("FAMILY"),
    COUPLE("COUPLE");

    private final String value;
}
