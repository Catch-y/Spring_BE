package umc.catchy.domain.placeReview.dao;

import org.springframework.data.domain.Slice;
import umc.catchy.domain.placeReview.dto.response.PostPlaceReviewResponse;
import umc.catchy.domain.reviewReport.dto.response.MyPageReviewsResponse;

import java.util.List;
import java.util.Optional;

public interface PlaceReviewRepositoryCustom {
    List<PostPlaceReviewResponse.placeReviewRatingResponseDTO> findRatingList(Long placeId);
    Slice<PostPlaceReviewResponse.newPlaceReviewResponseDTO> findPlaceReviewSliceByPlaceId(Long placeId, int pageSize, Long lastPlaceReviewId);
    Optional<Double> findAverageRatingByPlaceId(Long placeId);
    Slice<MyPageReviewsResponse.PlaceReviewDTO> getAllPlaceReviewByMemberId(Long memberId, int pageSize, Long lastPlaceReviewId);
}
