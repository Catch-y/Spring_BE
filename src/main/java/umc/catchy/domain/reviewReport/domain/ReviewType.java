package umc.catchy.domain.reviewReport.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReviewType {
    COURSE("COURSE"),
    PLACE("PLACE");

    private final String value;
}
