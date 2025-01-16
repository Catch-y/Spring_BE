package umc.catchy.domain.courseReview.converter;

import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.courseReview.domain.CourseReview;
import umc.catchy.domain.courseReview.dto.request.PostCourseReviewRequest;
import umc.catchy.domain.courseReview.dto.response.PostCourseReviewResponse;
import umc.catchy.domain.member.domain.Member;

import java.time.LocalDateTime;
import java.util.List;

public class CourseReviewConverter {

    public static PostCourseReviewResponse.newCourseReviewResponseDTO toNewCourseReviewResponseDTO(
            CourseReview courseReview,
            List<PostCourseReviewResponse.courseReviewImageResponseDTO> images,
            LocalDateTime visitedDate
    ){
        return PostCourseReviewResponse.newCourseReviewResponseDTO.builder()
                .reviewId(courseReview.getId())
                .comment(courseReview.getComment())
                .reviewImages(images)
                .visitedDate(visitedDate)
                .creatorNickname(courseReview.getMember().getNickname())
                .build();
    }

    public static CourseReview toCourseReview(Member member, Course course, PostCourseReviewRequest request){
        return CourseReview.builder()
                .comment(request.getComment())
                .member(member)
                .course(course)
                .build();
    }
}
