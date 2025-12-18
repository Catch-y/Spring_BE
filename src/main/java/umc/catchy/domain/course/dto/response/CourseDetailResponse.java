package umc.catchy.domain.course.dto.response;

import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.course.domain.CourseType;
import umc.catchy.domain.place.domain.Place;

import java.time.format.DateTimeFormatter;
import java.util.List;

public record CourseDetailResponse(
        Long courseId,
        String courseImage,
        String courseName,
        String courseDescription,
        CourseType courseType,
        Double rating,
        Integer reviewCount,
        String recommendTime,
        Long participantsNumber,
        Boolean isBookMarked,
        List<CoursePlaceInfo> placeInfos
) {
    public static CourseDetailResponse from(Course course, Integer reviewCount, Boolean isBookMarked, List<CoursePlaceInfo> placeInfos) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        String timeStr = course.getRecommendTimeStart().format(formatter) + " ~ " + course.getRecommendTimeEnd().format(formatter);

        return new CourseDetailResponse(
                course.getId(),
                course.getCourseImage(),
                course.getCourseName(),
                course.getCourseDescription(),
                course.getCourseType(),
                course.getRating(),
                reviewCount,
                timeStr,
                course.getParticipantsNumber(),
                isBookMarked,
                placeInfos
        );
    }

    public record CoursePlaceInfo(
            Long placeId,
            String placeName,
            BigCategory category,
            Double placeLatitude,
            Double placeLongitude,
            Boolean isVisited
    ) {
        public static CoursePlaceInfo of(Place place, Boolean isVisited) {
            return new CoursePlaceInfo(
                    place.getId(),
                    place.getPlaceName(),
                    place.getCategory().getBigCategory(),
                    place.getLatitude(),
                    place.getLongitude(),
                    isVisited
            );
        }
    }
}
