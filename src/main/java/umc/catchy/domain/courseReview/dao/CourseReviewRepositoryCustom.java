package umc.catchy.domain.courseReview.dao;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import umc.catchy.domain.courseReview.dto.response.PostCourseReviewResponse;

public interface CourseReviewRepositoryCustom {
    Slice<PostCourseReviewResponse.newCourseReviewResponseDTO> getAllCourseReviewByCourseId(Long courseId,int pageSize,Long lastReviewId);
}
