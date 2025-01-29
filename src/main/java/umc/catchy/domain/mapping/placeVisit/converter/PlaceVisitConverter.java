package umc.catchy.domain.mapping.placeVisit.converter;

import java.time.LocalDateTime;
import umc.catchy.domain.mapping.placeVisit.domain.PlaceVisit;
import umc.catchy.domain.mapping.placeVisit.dto.response.PlaceVisitedResponse;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.domain.Place;

public class PlaceVisitConverter {

    public static PlaceVisitedResponse toPlaceVisitResponse(PlaceVisit placeVisit) {
        return PlaceVisitedResponse.builder()
                .placeVisitId(placeVisit.getId())
                .visitedDate(placeVisit.getVisitedDate())
                .isVisited(placeVisit.isVisited())
                .build();
    }

    public static PlaceVisit toPlaceVisit(Place place, Member member) {
        return PlaceVisit.builder()
                .place(place)
                .member(member)
                .visitedDate(LocalDateTime.now())
                .isVisited(true)
                .isLiked(false)
                .build();
    }
}
