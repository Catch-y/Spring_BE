package umc.catchy.domain.placeReview.converter;

import java.time.LocalDate;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.domain.placeReview.domain.PlaceReview;
import umc.catchy.domain.placeReview.dto.request.PostPlaceReviewRequest;
import umc.catchy.domain.placeReview.dto.response.PostPlaceReviewResponse;

import java.util.List;

public class PlaceReviewConverter {

    public static PostPlaceReviewResponse.newPlaceReviewResponseDTO toNewPlaceReviewResponseDTO(PlaceReview placeReview, List<PostPlaceReviewResponse.placeReviewImageResponseDTO> images) {
        return PostPlaceReviewResponse.newPlaceReviewResponseDTO.builder()
                .reviewId(placeReview.getId())
                .comment(placeReview.getComment())
                .rating(placeReview.getRating())
                .reviewImages(images)
                .visitedDate(placeReview.getVisitedDate())
                .creatorNickname(placeReview.getMember().getNickname())
                .build();
    }

    public static PlaceReview toPlaceReview(Member member, Place place, PostPlaceReviewRequest request){
        return PlaceReview.builder()
                .comment(request.getComment())
                .rating(request.getRating())
                .visitedDate(request.getVisitedDate())
                .member(member)
                .place(place)
                .isReported(false)
                .build();
    }
}
