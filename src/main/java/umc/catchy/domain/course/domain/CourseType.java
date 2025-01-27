package umc.catchy.domain.course.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CourseType {
    AI("AI_GENERATED"),
    DIY("USER_CREATED");

    private final String value;
}
