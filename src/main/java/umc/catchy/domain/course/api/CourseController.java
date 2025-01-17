package umc.catchy.domain.course.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import umc.catchy.domain.course.dto.response.CourseInfoResponse;
import umc.catchy.domain.course.service.CourseService;
import umc.catchy.domain.mapping.memberCourse.dto.response.MemberCourseResponse;
import umc.catchy.domain.courseReview.dto.request.PostCourseReviewRequest;
import umc.catchy.domain.courseReview.dto.response.PostCourseReviewResponse;
import umc.catchy.domain.courseReview.service.CourseReviewService;
import umc.catchy.global.common.response.BaseResponse;
import umc.catchy.global.common.response.status.SuccessStatus;

import java.util.Collections;

@Tag(name = "Course", description = "코스 관련 API")
@RestController
@RequestMapping("/course")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final CourseReviewService courseReviewService;

    @Operation(summary = "코스 상세정보 조회 API", description = "코스 상세 화면에서 코스에 대한 상세정보를 나타내기 위한 정보 조회 기능입니다.")
    @GetMapping("/detail/{courseId}")
    public ResponseEntity<BaseResponse<CourseInfoResponse.getCourseInfoDTO>> getCourseInfo(
            @Parameter(description = "코스 ID", required = true)
            @PathVariable Long courseId
    ){
        CourseInfoResponse.getCourseInfoDTO response= courseService.getCourseDetails(courseId);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, response));
    }

    @Operation(summary = "내 코스 조회 API", description = "코스 탭에서 DIY/AI, 지역별로 사용자의 코스를 조회")
    @GetMapping("/course/search")
    public ResponseEntity<BaseResponse<List<MemberCourseResponse>>> getMemberCourses(
            @RequestParam(value = "type", defaultValue = "AI") String type,
            @RequestParam(value = "upperLocation", defaultValue = "all") String upperLocation,
            @RequestParam(value = "lowerLocation", defaultValue = "all") String lowerLocation
    ) {

        List<MemberCourseResponse> responses = courseService.getMemberCourses(type, upperLocation, lowerLocation);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, responses));
    }
      
    @Operation(summary = "코스 리뷰 작성 API", description = "코스 리뷰 작성을 위한 API, 멤버가 해당 코스의 과반수 이상의 장소에 방문 체크를 성공하였을 때 리뷰 작성 권한이 주어집니다.")
    @PostMapping(value = "/{courseId}/review", consumes = "multipart/form-data")
    public ResponseEntity<BaseResponse<PostCourseReviewResponse.newCourseReviewResponseDTO>> postCourseReview(
            @PathVariable Long courseId,
            @Valid @ModelAttribute PostCourseReviewRequest request
        ){
        //빈 이미지 리스트 처리
        if (request.getImages() == null || request.getImages().isEmpty()) {
            request.setImages(Collections.emptyList());
        }
        PostCourseReviewResponse.newCourseReviewResponseDTO response = courseReviewService.postNewCourseReview(courseId, request);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, response));
    }
}
