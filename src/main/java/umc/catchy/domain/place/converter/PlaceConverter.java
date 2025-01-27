package umc.catchy.domain.place.converter;

import java.util.List;
import java.util.Map;
import umc.catchy.domain.course.dto.response.CourseInfoResponse;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceInfoPreview;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceInfoPreviewResponse;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceInfoResponse;
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

    public static PlaceInfoPreview toPlaceInfoPreview(Place place, Long reviewCount) {
        String categoryName = "";
        Double rating = 0.0;

        if (place.getCategory() != null) {
            categoryName = place.getCategory().getName();
        }

        if (place.getRating() != null) {
            rating = place.getRating();
        }

        return PlaceInfoPreview.builder()
                .placeId(place.getId())
                .placeName(place.getPlaceName())
                .placeImage(place.getImageUrl())
                .category(categoryName)
                .roadAddress(place.getRoadAddress())
                .activeTime(place.getActiveTime())
                .rating(rating)
                .reviewCount(reviewCount)
                .build();
    }

    public static PlaceInfoResponse toPlaceInfoResponse (Place place, Long reviewCount, Boolean isVisited) {
        String categoryName = "";
        Double rating = 0.0;

        if (place.getCategory() != null) {
            categoryName = place.getCategory().getName();
        }

        if (place.getRating() != null) {
            rating = place.getRating();
        }

        return PlaceInfoResponse.builder()
                .placeId(place.getId())
                .imageUrl(place.getImageUrl())
                .placeName(place.getPlaceName())
                .placeDescription(place.getPlaceDescription())
                .categoryName(categoryName)
                .roadAddress(place.getRoadAddress())
                .activeTime(place.getActiveTime())
                .placeSite(place.getPlaceSite())
                .rating(rating)
                .isVisited(isVisited)
                .reviewCount(reviewCount)
                .build();
    }

    public static Place toPlace(Map<String, String> placeInfo) {
        return Place.builder()
                .poiId(Long.parseLong(placeInfo.get("id")))
                .placeName(placeInfo.get("name"))
                .imageUrl(placeInfo.get("image"))
                .placeDescription(placeInfo.get("desc"))
                .roadAddress(placeInfo.get("bldAddr"))
                .numberAddress(placeInfo.get("address"))
                .latitude(Double.parseDouble(placeInfo.get("lat")))
                .longitude(Double.parseDouble(placeInfo.get("lon")))
                .activeTime(placeInfo.get("additionalInfo"))
                .placeSite(placeInfo.get("homepageURL"))
                .rating(0.0)
                .build();
    }

    public static PlaceInfoPreviewResponse toPlaceInfoPreviewResponse(List<PlaceInfoPreview> placeInfoPreviews, Boolean isLast) {
        return PlaceInfoPreviewResponse.builder()
                .placeInfoPreviews(placeInfoPreviews)
                .isLast(isLast)
                .build();
    }
}
