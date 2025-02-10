package umc.catchy.domain.place.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceInfoPreviewSliceResponse;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceInfoSliceResponse;
import umc.catchy.domain.mapping.placeLike.dto.response.PlaceLikedResponse;
import umc.catchy.domain.mapping.placeLike.service.PlaceLikeService;
import umc.catchy.domain.mapping.placeVisit.dto.response.PlaceVisitedDateResponse;
import umc.catchy.domain.mapping.placeVisit.service.PlaceVisitService;
import umc.catchy.domain.place.service.PlaceService;
import umc.catchy.domain.placeReview.dto.request.PostPlaceReviewRequest;
import umc.catchy.domain.placeReview.dto.response.PostPlaceReviewResponse;
import umc.catchy.domain.placeReview.service.PlaceReviewService;
import umc.catchy.global.common.response.BaseResponse;
import umc.catchy.global.common.response.status.SuccessStatus;

import java.util.Collections;

@Tag(name = "Place", description = "장소 관련 API")
@RestController
@RequestMapping("/place")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceReviewService placeReviewService;
    private final PlaceVisitService placeVisitService;
    private final PlaceService placeService;
    private final PlaceLikeService placeLikeService;

    @Operation(summary = "장소 평점/리뷰 달기 API", description = "장소에 대해 평점/리뷰를 달기 위한 API입니다. 멤버가 해당 장소에 방문체크한 이후 평점/리뷰를 남길 수 있습니다.")
    @PostMapping(value = "/{placeId}/review", consumes = "multipart/form-data")
    public ResponseEntity<BaseResponse<PostPlaceReviewResponse.newPlaceReviewResponseDTO>> postPlaceReview(
            @PathVariable("placeId") Long placeId,
            @Valid @ModelAttribute PostPlaceReviewRequest request
            ){
        //빈 이미지 리스트 처리
        if (request.getImages() == null || request.getImages().isEmpty()) {
            request.setImages(Collections.emptyList());
        }

        PostPlaceReviewResponse.newPlaceReviewResponseDTO response
                = placeReviewService.postNewPlaceReview(request, placeId);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK,response));
    }

    @Operation(summary = "장소 좋아요 API", description = "사용자가 해당 장소를 좋아요로 설정합니다.")
    @PatchMapping("/{placeId}/like")
    public ResponseEntity<BaseResponse<PlaceLikedResponse>> toggleLike(@PathVariable("placeId") Long placeId) {
        PlaceLikedResponse response = placeLikeService.togglePlaceLiked(placeId);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK,response));
    }

    @Operation(summary = "장소 방문 날짜 리스트 조회 API", description = "사용자가 해당 장소를 방문 한 날짜들을 보여줍니다.")
    @GetMapping("/{placeId}/visit")
    public ResponseEntity<BaseResponse<PlaceVisitedDateResponse>> getPlaceVisitDate(@PathVariable("placeId") Long placeId) {
        PlaceVisitedDateResponse response = placeVisitService.getPlaceVisitDate(placeId);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK,response));
    }

    @Operation(summary = "장소리뷰 전체 조회 API", description = "해당 장소의 전체 리뷰를 보여줍니다.")
    @GetMapping("/{placeId}/review/all")
    public ResponseEntity<BaseResponse<PostPlaceReviewResponse.placeReviewAllResponseDTO>> getAllPlaceReviews(
            @PathVariable("placeId") Long placeId,
            @RequestParam int pageSize,
            @RequestParam(required = false) Long lastPlaceReviewId
    ) {
        PostPlaceReviewResponse.placeReviewAllResponseDTO response = placeReviewService.getAllPlaceReviews(placeId, pageSize, lastPlaceReviewId);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK,response));
    }

    @Operation(summary = "사용자 장소 추천 API", description = "사용자의 행동데이터 기반으로 장소를 추천")
    @GetMapping("/home/recommend-places")
    public ResponseEntity<BaseResponse<PlaceInfoPreviewSliceResponse>> getRecommendPlaces(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam int pageSize,
            @RequestParam int page
    ) {
        PlaceInfoPreviewSliceResponse response = placeService.recommendPlaces(latitude, longitude, pageSize, page);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, response));
    }

    @Operation(summary = "장소 검색 API", description = "장소이름/카테고리를 통해 장소 리스트 반환")
    @GetMapping("/home/search")
    public ResponseEntity<BaseResponse<PlaceInfoSliceResponse>> getSearchPlaces(
            @RequestParam(required = false) String keyword,
            @RequestParam int pageSize,
            @RequestParam(required = false) Long lastPlaceId
    ) {
        PlaceInfoSliceResponse response = placeService.searchPlaceByCategoryOrName(pageSize,keyword,lastPlaceId);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, response));
    }
}
