package umc.catchy.domain.mapping.placeCourse.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceInfoPreviewResponse;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceInfoResponse;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceInfoSliceResponse;
import umc.catchy.domain.mapping.placeCourse.service.PlaceCourseService;
import umc.catchy.global.common.response.BaseResponse;
import umc.catchy.global.common.response.status.SuccessStatus;

@Tag(name = "PlaceCourse", description = "코스/장소 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/course/place")
public class PlaceCourseController {

    private final PlaceCourseService placeCourseService;

    @GetMapping("/current")
    @Operation(summary = "내 위치 기반 장소 검색 API", description = "사용자 반경 5km 이내에 사용자 키워드 관련 장소를 불러온다.")
    public ResponseEntity<BaseResponse<PlaceInfoPreviewResponse>> searchPlacesByMemberLocation(
            @RequestParam String searchKeyword,
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam Integer page
    ){
        PlaceInfoPreviewResponse response = placeCourseService.getPlacesByLocation(searchKeyword, latitude, longitude, page);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, response));
    }

    @GetMapping("/region")
    @Operation(summary = "지역명 기반 장소 검색 API", description = "지역명이 포함된 키워드를 받아 관련 장소를 검색")
    public ResponseEntity<BaseResponse<PlaceInfoPreviewResponse>> searchPlacesByLocation(
            @RequestParam String searchKeyword,
            @RequestParam Integer page
    ){
        PlaceInfoPreviewResponse response = placeCourseService.getPlacesByLocation(searchKeyword, null, null, page);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, response));
    }

    @GetMapping
    @Operation(summary = "지역 상세 화면 API", description = "지도에서 장소 검색 후 클릭하면 나오는 상세 화면")
    public ResponseEntity<BaseResponse<PlaceInfoResponse>> getPlaceInfoDetail(@RequestParam Long placeId){
        PlaceInfoResponse response = placeCourseService.getPlaceResponseByPlaceId(placeId);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, response));
    }

    @Operation(summary = "좋아요한 장소 무한 스크롤 API", description = "좋아요한 장소 정보들을 무한 스크롤로 보여줍니다.")
    @GetMapping("/mypage/like")
    public BaseResponse<PlaceInfoSliceResponse> findAllCourseByBookmarked(@RequestParam int pageSize,
                                                                            @RequestParam(required = false) Long lastPlaceId) {
        PlaceInfoSliceResponse response = placeCourseService.searchLikedPlace(pageSize, lastPlaceId);
        return BaseResponse.onSuccess(SuccessStatus._OK,response);
    }
}
