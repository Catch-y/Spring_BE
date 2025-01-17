package umc.catchy.domain.place.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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

}
