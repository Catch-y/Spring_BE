package umc.catchy.domain.mapping.memberCourse.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Slice;

import java.util.List;

public record MemberCourseSliceResponse(
    @Schema(description = "코스 데이터") List<MemberCourseResponse> content,
    @Schema(description = "마지막 페이지 여부") Boolean last){

    public static MemberCourseSliceResponse from(Slice<MemberCourseResponse> memberCourseResponses) {
        return new MemberCourseSliceResponse(memberCourseResponses.getContent(),memberCourseResponses.isLast());
    }
}
