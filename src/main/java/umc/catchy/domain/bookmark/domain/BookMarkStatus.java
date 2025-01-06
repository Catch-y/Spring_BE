package umc.catchy.domain.bookmark.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BookMarkStatus {
    MARKED("MARKED"),
    UNMARKED("UNMARKED");

    private final String value;
}
