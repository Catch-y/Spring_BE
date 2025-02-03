package umc.catchy.domain.place.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import umc.catchy.domain.mapping.placeVisit.dto.response.PlaceLikedResponse;
import umc.catchy.domain.mapping.placeVisit.dto.response.PlaceVisitedDateResponse;
import umc.catchy.domain.mapping.placeVisit.service.PlaceVisitService;
import umc.catchy.domain.placeReview.dto.request.PostPlaceReviewRequest;
import umc.catchy.domain.placeReview.dto.response.PostPlaceReviewResponse;
import umc.catchy.domain.placeReview.service.PlaceReviewService;
import umc.catchy.global.common.response.BaseResponse;
import umc.catchy.global.common.response.status.SuccessStatus;

import java.time.LocalDate;
import java.util.Collections;

@Tag(name = "Place", description = "장소 관련 API")
@RestController
@RequestMapping("/place")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceReviewService placeReviewService;
    private final PlaceVisitService placeVisitService;

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
        PlaceLikedResponse response = placeVisitService.togglePlaceLiked(placeId);
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
            @RequestParam(required = false) LocalDate lastPlaceReviewDate,
            @RequestParam(required = false) Long lastPlaceReviewId
    ) {
        PostPlaceReviewResponse.placeReviewAllResponseDTO response = placeReviewService.getAllPlaceReviews(placeId, pageSize, lastPlaceReviewDate, lastPlaceReviewId);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK,response));
    }


}
