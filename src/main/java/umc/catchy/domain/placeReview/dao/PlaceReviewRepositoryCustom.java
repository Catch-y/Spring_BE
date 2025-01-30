package umc.catchy.domain.placeReview.dao;

import org.springframework.data.domain.Slice;
import umc.catchy.domain.placeReview.domain.PlaceReview;
import umc.catchy.domain.placeReview.dto.response.PostPlaceReviewResponse;

import java.util.List;

public interface PlaceReviewRepositoryCustom {
    List<PostPlaceReviewResponse.placeReviewRatingResponseDTO> findRatingList(Long placeId);
    Slice<PostPlaceReviewResponse.newPlaceReviewResponseDTO> findPlaceReviewSliceByPlaceId(Long placeId, int pageSize, Long lastPlaceReviewId);
    Float findAverageRatingByPlaceId(Long placeId);
}
