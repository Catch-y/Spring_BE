package umc.catchy.domain.mapping.placeCourse.api;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceInfoResponse;
import umc.catchy.domain.mapping.placeCourse.service.PlaceCourseService;
import umc.catchy.global.common.response.BaseResponse;
import umc.catchy.global.common.response.status.SuccessStatus;

@RestController
@RequiredArgsConstructor
public class PlaceCourseController {
    private final PlaceCourseService placeCourseService;

    @Operation(summary = "좋아요한 장소 무한 스크롤 API", description = "좋아요한 장소 정보들을 무한 스크롤로 보여줍니다.")
    @GetMapping("/mypage/like")
    public BaseResponse<Slice<PlaceInfoResponse>> findAllCourseByBookmarked(@RequestParam int pageSize,
                                                                            @RequestParam(required = false) Long lastPlaceId) {
        Slice<PlaceInfoResponse> response = placeCourseService.searchLikedPlace(pageSize, lastPlaceId);
        return BaseResponse.onSuccess(SuccessStatus._OK,response);
    }
}
