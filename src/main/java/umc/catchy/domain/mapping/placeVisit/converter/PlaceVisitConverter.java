package umc.catchy.domain.mapping.placeVisit.converter;

import java.time.LocalDate;
import umc.catchy.domain.course.domain.Course;
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

    public static PlaceVisit toPlaceVisit(Course course, Place place, Member member) {
        return PlaceVisit.builder()
                .course(course)
                .place(place)
                .member(member)
                .visitedDate(LocalDate.now())
                .isVisited(true)
                .build();
    }
}
