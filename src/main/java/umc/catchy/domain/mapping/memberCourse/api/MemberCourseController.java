package umc.catchy.domain.mapping.memberCourse.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import umc.catchy.domain.course.domain.CourseType;
import umc.catchy.domain.mapping.memberCourse.dto.response.CourseBookmarkResponse;
import umc.catchy.domain.mapping.memberCourse.dto.response.MemberCourseSliceResponse;
import umc.catchy.domain.mapping.memberCourse.service.MemberCourseService;
import umc.catchy.global.common.response.BaseResponse;
import umc.catchy.global.common.response.status.SuccessStatus;

@RestController
@RequiredArgsConstructor
public class MemberCourseController {
    private final MemberCourseService memberCourseService;

    @Operation(summary = "내 코스 조회 API", description = "코스 탭에서 DIY/AI, 지역별로 사용자의 코스를 최신순으로 조회")
    @GetMapping("/course/search")
    public ResponseEntity<BaseResponse<MemberCourseSliceResponse>> getMemberCourses(
            @Parameter(description = "AI/DIY 선택", required = true)
            @RequestParam(value = "type") CourseType type,
            @RequestParam(value = "upperLocation", defaultValue = "all") String upperLocation,
            @RequestParam(value = "lowerLocation", defaultValue = "all") String lowerLocation,
            @RequestParam(required = false) Long lastId
    ) {
        MemberCourseSliceResponse response = memberCourseService.getMemberCourses(type, upperLocation, lowerLocation, lastId);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, response));
    }

    @Operation(summary = "코스 북마크 API", description = "사용자가 해당 코스를 북마크합니다.")
    @PatchMapping("/course/{courseId}/bookmark")
    public ResponseEntity<BaseResponse<CourseBookmarkResponse>> toggleBookmark(@PathVariable Long courseId) {
        CourseBookmarkResponse response = memberCourseService.toggleBookmark(courseId);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, response));
    }

    @Operation(summary = "북마크된 코스 무한 스크롤 API", description = "북마크된 코스 정보들을 무한 스크롤로 보여줍니다.")
    @GetMapping("/mypage/bookmark")
    public BaseResponse<MemberCourseSliceResponse> findAllCourseByBookmarked(@RequestParam int pageSize,
                                                                             @RequestParam(required = false) Long lastCourseId) {
        MemberCourseSliceResponse response = memberCourseService.findAllCourseByBookmarked(pageSize, lastCourseId);
        return BaseResponse.onSuccess(SuccessStatus._OK,response);
    }
}
