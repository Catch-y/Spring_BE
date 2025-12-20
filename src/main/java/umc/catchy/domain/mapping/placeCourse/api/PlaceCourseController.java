package umc.catchy.domain.mapping.placeCourse.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import umc.catchy.domain.mapping.placeCourse.dto.request.PlaceSearchRequest;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceDetailResponse;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlacePreviewResponse;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceResponse;
import umc.catchy.domain.mapping.placeCourse.service.PlaceCourseFacade;
import umc.catchy.domain.mapping.placeLike.dto.response.LikedPlaceSliceResponse;
import umc.catchy.global.common.dto.SliceResponse;
import umc.catchy.global.common.response.BaseResponse;
import umc.catchy.global.common.response.status.SuccessStatus;

@Tag(name = "PlaceCourse", description = "코스/장소 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/course/place")
public class PlaceCourseController {

    private final PlaceCourseFacade placeCourseFacade;

    @PostMapping("/search")
    @Operation(summary = "프론트엔드 장소 검색 API", description = "Apple Maps에서 받은 장소 리스트를 처리합니다.")
    public ResponseEntity<BaseResponse<List<PlacePreviewResponse>>> searchPlacesByFrontend(
            @RequestBody @Valid List<PlaceSearchRequest> requests
    ) {
        List<PlacePreviewResponse> response = placeCourseFacade.getPlacesByFrontend(requests);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, response));
    }

    @GetMapping("/{placeId}")
    @Operation(summary = "장소 상세 화면 API", description = "지도에서 장소 검색 후 클릭하면 나오는 상세 화면")
    public ResponseEntity<BaseResponse<PlaceDetailResponse>> getPlaceDetail(@PathVariable Long placeId){
        PlaceDetailResponse response = placeCourseFacade.getPlaceDetailByPlaceId(placeId);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, response));
    }

    @Operation(summary = "좋아요한 장소 무한 스크롤 API", description = "좋아요한 장소 정보들을 무한 스크롤로 보여줍니다.")
    @GetMapping("/mypage/like")
    public BaseResponse<LikedPlaceSliceResponse> findAllCourseByBookmarked(
            @RequestParam int pageSize,
            @RequestParam(required = false) Long lastPlaceId
    ) {
        LikedPlaceSliceResponse response = placeCourseFacade.searchLikedPlace(pageSize, lastPlaceId);
        return BaseResponse.onSuccess(SuccessStatus._OK, response);
    }
}
