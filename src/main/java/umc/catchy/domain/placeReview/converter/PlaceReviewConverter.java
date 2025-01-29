package umc.catchy.domain.placeReview.converter;

import java.time.LocalDate;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.domain.placeReview.domain.PlaceReview;
import umc.catchy.domain.placeReview.dto.request.PostPlaceReviewRequest;
import umc.catchy.domain.placeReview.dto.response.PostPlaceReviewResponse;

import java.time.LocalDateTime;
import java.util.List;

public class PlaceReviewConverter {

    public static PostPlaceReviewResponse.newPlaceReviewResponseDTO toNewPlaceReviewResponseDTO(PlaceReview placeReview, List<PostPlaceReviewResponse.placeReviewImageResponseDTO> images, LocalDate visitedDate) {
        return PostPlaceReviewResponse.newPlaceReviewResponseDTO.builder()
                .reviewId(placeReview.getId())
                .comment(placeReview.getComment())
                .rating(placeReview.getRating())
                .reviewImages(images)
                .visitedDate(visitedDate)
                .creatorNickname(placeReview.getMember().getNickname())
                .build();
    }

    public static PlaceReview toPlaceReview(Member member, Place place, PostPlaceReviewRequest request){
        return PlaceReview.builder()
                .comment(request.getComment())
                .rating(request.getRating())
                .member(member)
                .place(place)
                .build();
    }
}
