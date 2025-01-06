package umc.catchy.domain.course.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CourseType {
    AI_GENERATED("AI_GENERATED"),
    USER_CREATED("USER_CREATED");

    private final String value;
}
