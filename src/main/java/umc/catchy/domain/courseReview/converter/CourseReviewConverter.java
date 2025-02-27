package umc.catchy.domain.courseReview.converter;

import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.courseReview.domain.CourseReview;
import umc.catchy.domain.courseReview.dto.request.PostCourseReviewRequest;
import umc.catchy.domain.courseReview.dto.response.PostCourseReviewResponse;
import umc.catchy.domain.member.domain.Member;

import java.time.LocalDate;
import java.util.List;

public class CourseReviewConverter {

    public static PostCourseReviewResponse.newCourseReviewResponseDTO toNewCourseReviewResponseDTO(
            CourseReview courseReview,
            List<PostCourseReviewResponse.courseReviewImageResponseDTO> images
    ){
        return PostCourseReviewResponse.newCourseReviewResponseDTO.builder()
                .reviewId(courseReview.getId())
                .comment(courseReview.getComment())
                .reviewImages(images)
                .createdAt(courseReview.getCreatedAt())
                .creatorNickname(courseReview.getMember().getNickname())
                .build();
    }

    public static CourseReview toCourseReview(Member member, Course course, PostCourseReviewRequest request){
        return CourseReview.builder()
                .comment(request.getComment())
                .member(member)
                .course(course)
                .createdAt(LocalDate.now())
                .isReported(false)
                .build();
    }
}
