package umc.catchy.domain.reviewReport.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.catchy.domain.courseReview.dao.CourseReviewRepository;
import umc.catchy.domain.courseReview.domain.CourseReview;
import umc.catchy.domain.placeReview.dao.PlaceReviewRepository;
import umc.catchy.domain.placeReview.domain.PlaceReview;
import umc.catchy.domain.reviewReport.converter.ReviewReportConverter;
import umc.catchy.domain.reviewReport.dao.ReviewReportRepository;
import umc.catchy.domain.reviewReport.domain.ReviewReport;
import umc.catchy.domain.reviewReport.dto.request.PostReviewReportRequest;
import umc.catchy.domain.reviewReport.dto.response.PostReviewReportResponse;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;

import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
public class ReviewReportService {

    private final ReviewReportRepository reviewReportRepository;
    private final CourseReviewRepository courseReviewRepository;
    private final PlaceReviewRepository placeReviewRepository;

    //리뷰 신고하기
    //TODO 해당 리뷰의 state 변경
    public PostReviewReportResponse postReviewReport(Long reviewId, PostReviewReportRequest request) {
        if(Objects.equals(request.getReviewType(), "PLACE")){
            PlaceReview placeReview = placeReviewRepository.findById(reviewId)
                    .orElseThrow(()-> new GeneralException(ErrorStatus.PLACE_REVIEW_NOT_FOUND));

            ReviewReport newReport = ReviewReportConverter.toPlaceReviewReport(request, placeReview);
            reviewReportRepository.save(newReport);
            return ReviewReportConverter.toPostReviewReportResponse(newReport);
        }
        else if(Objects.equals(request.getReviewType(), "COURSE")){
            CourseReview courseReview = courseReviewRepository.findById(reviewId)
                    .orElseThrow(()-> new GeneralException(ErrorStatus.COURSE_REVIEW_NOT_FOUND));

            ReviewReport newReport = ReviewReportConverter.toCourseReviewReport(request, courseReview);
            reviewReportRepository.save(newReport);
            return ReviewReportConverter.toPostReviewReportResponse(newReport);
        }
        else throw new GeneralException(ErrorStatus._BAD_REQUEST);
    }
}
