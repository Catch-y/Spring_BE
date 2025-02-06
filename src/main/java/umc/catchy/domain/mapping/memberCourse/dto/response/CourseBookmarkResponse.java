package umc.catchy.domain.mapping.memberCourse.dto.response;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseBookmarkResponse {
    Long memberCourseId;
    boolean bookmarked;
}
