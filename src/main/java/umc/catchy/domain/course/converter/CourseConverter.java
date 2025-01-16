package umc.catchy.domain.course.converter;

import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.course.dto.response.CourseInfoResponse;

import java.util.List;

public class CourseConverter {

    public static CourseInfoResponse.getCourseInfoDTO toCourseInfoDTO(
            Course course,
            Integer reviewCount,
            String recommendTime,
            List<CourseInfoResponse.getPlaceInfoOfCourseDTO> placeInfoOfCourseDTOS
    ){
        return CourseInfoResponse.getCourseInfoDTO.builder()
                .courseId(course.getId())
                .courseImage(course.getCourseImage())
                .courseName(course.getCourseName())
                .courseDescription(course.getCourseDescription())
                .courseType(course.getCourseType())
                .rating(course.getRating())
                .reviewCount(reviewCount)
                .recommendTime(recommendTime)
                .participantsNumber(course.getParticipantsNumber())
                .placeInfos(placeInfoOfCourseDTOS)
                .build();
    }
}
