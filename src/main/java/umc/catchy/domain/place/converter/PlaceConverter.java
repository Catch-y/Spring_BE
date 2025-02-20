package umc.catchy.domain.place.converter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import umc.catchy.domain.course.dto.response.CourseInfoResponse;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceInfoDetail;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceInfoPreview;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceInfoPreviewResponse;
import umc.catchy.domain.place.domain.Place;

public class PlaceConverter {

    public static CourseInfoResponse.getPlaceInfoOfCourseDTO toPlaceInfoOfCourseDTO(Place place, Boolean isVisited) {
        return CourseInfoResponse.getPlaceInfoOfCourseDTO
                .builder()
                .placeId(place.getId())
                .placeName(place.getPlaceName())
                .category(place.getCategory().getBigCategory())
                .placeLatitude(place.getLatitude())
                .placeLongitude(place.getLongitude())
                .isVisited(isVisited)
                .build();
    }

    public static PlaceInfoPreview toPlaceInfoPreview(Place place, Long reviewCount, Boolean isLiked) {
        String categoryName = null;
        Double rating = 0.0;

        if (place.getCategory() != null) {
            categoryName = place.getCategory().getBigCategory().getValue();
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
                .placeLatitude(place.getLatitude())
                .placeLongitude(place.getLongitude())
                .build();
    }

    public static PlaceInfoDetail toPlaceInfoDetail (Place place, Long reviewCount, Boolean isVisited, Boolean isLiked) {
        String categoryName = null;
        Double rating = 0.0;

        if (place.getCategory() != null) {
            categoryName = place.getCategory().getName();
        }

        if (place.getRating() != null) {
            rating = place.getRating();
        }

        return PlaceInfoDetail.builder()
                .placeId(place.getId())
                .imageUrl(place.getImageUrl())
                .placeName(place.getPlaceName())
                .categoryName(categoryName)
                .roadAddress(place.getRoadAddress())
                .activeTime(place.getActiveTime())
                .placeSite(place.getPlaceSite())
                .rating(rating)
                .reviewCount(reviewCount)
                .placeLatitude(place.getLatitude())
                .placeLongitude(place.getLongitude())
                .isVisited(isVisited)
                .liked(isLiked)
                .build();
    }

    public static Place toPlace(Map<String, String> placeInfo) {
        List<String> parsedTime = parsingTime(placeInfo.get("additionalInfo"));

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
                .startTime(parsedTime.isEmpty() ? null : formatTime(parsedTime.get(0)))
                .endTime(parsedTime.isEmpty() ? null : formatTime(parsedTime.get(1)))
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

    private static List<String> parsingTime(String activeTime) {
        List<String> timeRange = new ArrayList<>();

        Pattern pattern = Pattern.compile("\\b\\d{2}:\\d{2}~\\d{2}:\\d{2}\\b");
        Matcher matcher = pattern.matcher(activeTime);

        while (matcher.find()) {
            timeRange = Arrays.stream(matcher.group().split("~")).toList();
        }

        return timeRange;
    }

    private static LocalTime formatTime(String time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        return LocalTime.parse(time, formatter);
    }
}
