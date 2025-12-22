package umc.catchy.domain.mapping.memberCourse.dto.response;

public record CourseBookmarkResponse(
        Long memberCourseId,
        boolean bookmarked
) {
    public static CourseBookmarkResponse of(Long memberCourseId, boolean bookmarked) {
        return new CourseBookmarkResponse(memberCourseId, bookmarked);
    }
}
