package umc.catchy.domain.course.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import umc.catchy.domain.course.domain.CourseType;

import java.util.List;

public class CourseInfoResponse {

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class getCourseInfoDTO{
        Long courseId;
        String courseImage;
        String courseName;
        String courseDescription;
        CourseType courseType;
        Double rating;
        Integer reviewCount;
        String recommendTime;
        Long participantsNumber;
        Boolean isBookMarked;
        List<getPlaceInfoOfCourseDTO> placeInfos;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class getPlaceInfoOfCourseDTO{
        Long placeId;
        String placeName;
        Double placeLatitude;
        Double placeLongitude;
        Boolean isVisited;
    }
}
