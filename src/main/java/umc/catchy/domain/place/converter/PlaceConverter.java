package umc.catchy.domain.place.converter;

import umc.catchy.domain.course.dto.response.CourseInfoResponse;
import umc.catchy.domain.place.domain.Place;

public class PlaceConverter {

    public static CourseInfoResponse.getPlaceInfoOfCourseDTO toPlaceInfoOfCourseDTO(Place place, Boolean isVisited) {
        return CourseInfoResponse.getPlaceInfoOfCourseDTO
                .builder()
                .placeId(place.getId())
                .placeName(place.getPlaceName())
                .placeLatitude(place.getLatitude())
                .placeLongitude(place.getLongitude())
                .isVisited(isVisited)
                .build();
    }
}
