package umc.catchy.domain.placeReview.dao;

import umc.catchy.domain.placeReview.domain.PlaceReview;

import java.util.List;

public interface PlaceReviewRepositoryCustom {
    List<PlaceReview> findAllReviewsByPlaceId(Long placeId);
}
