package umc.catchy.domain.mapping.placeCourse.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceInfoResponse;
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
    public BaseResponse<List<PlaceInfoResponse>> searchPlacesByMemberLocation(
            @RequestParam String searchKeyword,
            @RequestParam Float latitude,
            @RequestParam Float longitude) {

        return BaseResponse.onSuccess(SuccessStatus._OK, placeCourseService.getPlacesByMemberLocation(searchKeyword, latitude, longitude));
    }

    @GetMapping("/region")
    @Operation(summary = "지역명 기반 장소 검색 API", description = "지역명이 포함된 키워드를 받아 관련 장소를 검색")
    public void searchPlacesByLocation() {

    }

}
