package umc.catchy.domain.like.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LikeStatus {
    LIKED("LIKED"),
    UNLIKED("UNLIKED");

    private final String value;
}
