package umc.catchy.domain.courseReview.dao;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import umc.catchy.domain.courseReview.dto.response.PostCourseReviewResponse;
import umc.catchy.domain.reviewReport.dto.response.MyPageReviewsResponse;

public interface CourseReviewRepositoryCustom {
    Slice<PostCourseReviewResponse.newCourseReviewResponseDTO> getAllCourseReviewByCourseId(Long courseId,int pageSize,Long lastReviewId);
    Slice<MyPageReviewsResponse.CourseReviewDTO> getAllCourseReviewByMemberId(Long memberId, int pageSize, Long lastReviewId);
}
