package umc.catchy.domain.course.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import umc.catchy.domain.course.dto.response.CourseInfoResponse;
import umc.catchy.domain.course.service.CourseService;
import umc.catchy.global.common.response.BaseResponse;
import umc.catchy.global.common.response.status.SuccessStatus;

@Tag(name = "Course", description = "코스 관련 API")
@RestController
@RequestMapping("/course")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @Operation(summary = "코스 상세정보 조회 API", description = "코스 상세 화면에서 코스에 대한 상세정보를 나타내기 위한 정보 조회 기능입니다.")
    @GetMapping("/detail/{courseId}")
    public ResponseEntity<BaseResponse<CourseInfoResponse.getCourseInfoDTO>> getCourseInfo(
            @Parameter(description = "코스 ID", required = true)
            @PathVariable Long courseId
    ){
        CourseInfoResponse.getCourseInfoDTO response= courseService.getCourseDetails(courseId);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, response));
    }
}
