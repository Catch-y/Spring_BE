package umc.catchy.domain.course.converter;

import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.course.dto.response.CourseInfoResponse;

import java.util.List;

public class CourseConverter {

    public static CourseInfoResponse.getCourseInfoDTO toCourseInfoDTO(Course course, List<CourseInfoResponse.getPlaceInfoOfCourseDTO> placeInfoOfCourseDTOS){
        //TODO rating, reviewCount, recommendTime 설정 필요
        return CourseInfoResponse.getCourseInfoDTO.builder()
                .courseId(course.getId())
                .courseImage(course.getCourseImage())
                .courseName(course.getCourseName())
                .courseDescription(course.getCourseDescription())
                .courseType(course.getCourseType())
                .participantsNumber(course.getParticipantsNumber())
                .placeInfos(placeInfoOfCourseDTOS)
                .build();
    }
}
