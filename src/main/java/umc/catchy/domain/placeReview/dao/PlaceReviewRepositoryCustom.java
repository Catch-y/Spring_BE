package umc.catchy.domain.placeReview.dao;

import org.springframework.data.domain.Slice;
import umc.catchy.domain.placeReview.dto.response.PlaceReviewResponse;
import umc.catchy.domain.placeReview.dto.response.PlaceReviewRatingResponse;
import umc.catchy.domain.reviewReport.dto.response.MyPageReviewsResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PlaceReviewRepositoryCustom {
    List<PlaceReviewRatingResponse> findRatingList(Long placeId);
    Slice<PlaceReviewResponse> findPlaceReviewSliceByPlaceId(Long placeId, int pageSize, LocalDate lastPlaceReviewDate, Long lastPlaceReviewId);
    Optional<Double> findAverageRatingByPlaceId(Long placeId);
    Slice<MyPageReviewsResponse.PlaceReviewDTO> getAllPlaceReviewByMemberId(Long memberId, int pageSize, LocalDate lastPlaceReviewDate, Long lastPlaceReviewId);
    Map<Long, Long> countReviewByPlaceIds(List<Long> placeIds);
}
