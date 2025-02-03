package umc.catchy.domain.course.converter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.course.domain.CourseType;
import umc.catchy.domain.course.dto.request.CourseCreateRequest;
import umc.catchy.domain.course.dto.response.CourseInfoResponse;

import java.util.List;
import java.util.stream.Collectors;

import umc.catchy.domain.course.dto.response.PopularCourseInfoResponse;
import umc.catchy.domain.member.domain.Member;

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

    public static Course toCourse(CourseCreateRequest request, String courseImageUrl, Member member) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime recommendTimeStart = LocalTime.parse(request.getRecommendTimeStart(), formatter);
        LocalTime recommendTImeEnd = LocalTime.parse(request.getRecommendTimeEnd(), formatter);

        return Course.builder()
                .courseName(request.getCourseName())
                .courseDescription(request.getCourseDescription())
                .courseImage(courseImageUrl)
                .courseType(CourseType.DIY)
                .recommendTimeStart(recommendTimeStart)
                .recommendTimeEnd(recommendTImeEnd)
                .member(member)
                .participantsNumber(0L)
                .hasReview(false)
                .build();
    }

    public static List<PopularCourseInfoResponse> toPopularCourseInfoResponseList(List<Course> courses) {
        return courses.stream()
                .map(course ->
                    PopularCourseInfoResponse.builder()
                            .courseId(course.getId())
                            .courseImage(course.getCourseImage())
                            .courseName(course.getCourseName())
                            .build()
                ).collect(Collectors.toList());
    }


}
