package umc.catchy.domain.mapping.memberCourse.dto.response;

public record CourseBookmarkResponse(
        Long memberCourseId,
        boolean bookmarked
) {
}
