package umc.catchy.domain.mapping.memberCourse.api;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import umc.catchy.domain.mapping.memberCourse.dto.response.MemberCourseResponse;
import umc.catchy.domain.mapping.memberCourse.service.MemberCourseService;
import umc.catchy.global.common.response.BaseResponse;
import umc.catchy.global.common.response.status.SuccessStatus;

@RestController
@RequiredArgsConstructor
public class MemberCourseController {
    private final MemberCourseService memberCourseService;

    @Operation(summary = "북마크된 코스 무한 스크롤 API", description = "북마크된 코스 정보들을 무한 스크롤로 보여줍니다.")
    @GetMapping("/mypage/bookmark")
    public BaseResponse<Slice<MemberCourseResponse>> findAllCourseByBookmarked(@RequestParam int pageSize,
                                                                                               @RequestParam(required = false) Long lastCourseId) {
        Slice<MemberCourseResponse> response = memberCourseService.findAllCourseByBookmarked(pageSize, lastCourseId);
        return BaseResponse.onSuccess(SuccessStatus._OK,response);
    }
}
