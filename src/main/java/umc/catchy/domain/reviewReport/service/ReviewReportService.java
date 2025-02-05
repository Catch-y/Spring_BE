package umc.catchy.domain.reviewReport.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.catchy.domain.courseReview.dao.CourseReviewRepository;
import umc.catchy.domain.courseReview.domain.CourseReview;
import umc.catchy.domain.courseReviewImage.dao.CourseReviewImageRepository;
import umc.catchy.domain.courseReviewImage.domain.CourseReviewImage;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.placeReview.dao.PlaceReviewRepository;
import umc.catchy.domain.placeReview.domain.PlaceReview;
import umc.catchy.domain.placeReviewImage.dao.PlaceReviewImageRepository;
import umc.catchy.domain.placeReviewImage.domain.PlaceReviewImage;
import umc.catchy.domain.reviewReport.converter.ReviewReportConverter;
import umc.catchy.domain.reviewReport.dao.ReviewReportRepository;
import umc.catchy.domain.reviewReport.domain.ReviewReport;
import umc.catchy.domain.reviewReport.domain.ReviewType;
import umc.catchy.domain.reviewReport.dto.request.PostReviewReportRequest;
import umc.catchy.domain.reviewReport.dto.response.DeleteReviewResponse;
import umc.catchy.domain.reviewReport.dto.response.MyPageReviewsResponse;
import umc.catchy.domain.reviewReport.dto.response.PostReviewReportResponse;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;
import umc.catchy.infra.aws.s3.AmazonS3Manager;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
public class ReviewReportService {

    private final ReviewReportRepository reviewReportRepository;
    private final CourseReviewRepository courseReviewRepository;
    private final CourseReviewImageRepository courseReviewImageRepository;
    private final PlaceReviewRepository placeReviewRepository;
    private final PlaceReviewImageRepository placeReviewImageRepository;
    private final MemberRepository memberRepository;
    private final AmazonS3Manager s3Manager;

    //리뷰 타입 ENUM 변환
    private ReviewType parseReviewType(String reviewType) {
        return Arrays.stream(ReviewType.values())
                .filter(t -> t.name().equalsIgnoreCase(reviewType))
                .findFirst()
                .orElseThrow(()-> new GeneralException(ErrorStatus._BAD_REQUEST, "리뷰 타입은 COURSE 또는 PLACE 입니다."));
    }

    //마이페이지 : 내 리뷰 조회
    public MyPageReviewsResponse.ReviewsDTO getMyReviews(String reviewType, int pageSize, Long lastReviewId){
        return null;
    }

    //리뷰 신고하기
    //TODO 해당 리뷰의 state 변경
    //TODO report 중복 판단
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

    //리뷰 이미지 삭제 : PLACE
    private void DeletePlaceReviewImages(PlaceReview placeReview){
        List<PlaceReviewImage> images = placeReviewImageRepository.findAllByPlaceReview(placeReview);
        images.forEach(image -> s3Manager.deleteImage(image.getImageUrl()));
        placeReviewImageRepository.deleteAllByPlaceReview(placeReview);
    }

    //리뷰 이미지 삭제 : COURSE
    private void DeleteCourseReviewImages(CourseReview courseReview){
        List<CourseReviewImage> images = courseReviewImageRepository.findAllByCourseReview(courseReview);
        images.forEach(image -> s3Manager.deleteImage(image.getImageUrl()));
        courseReviewImageRepository.deleteAllByCourseReview(courseReview);
    }

    //리뷰 삭제하기
    public DeleteReviewResponse deleteReview(Long reviewId, String reviewType){
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        ReviewType type = parseReviewType(reviewType);

        if(type==ReviewType.PLACE){
            PlaceReview placeReview = placeReviewRepository.findById(reviewId)
                    .orElseThrow(()-> new GeneralException(ErrorStatus.PLACE_REVIEW_NOT_FOUND));

            if(!placeReview.getMember().equals(member)){
                throw new GeneralException(ErrorStatus.REVIEW_DELETE_INVALID);
            }
            DeletePlaceReviewImages(placeReview);
            reviewReportRepository.deleteAllByPlaceReview(placeReview);
            placeReviewRepository.delete(placeReview);
        }
        else {
            CourseReview courseReview = courseReviewRepository.findById(reviewId)
                    .orElseThrow(()-> new GeneralException(ErrorStatus.COURSE_REVIEW_NOT_FOUND));

            if(!courseReview.getMember().equals(member)){
                throw new GeneralException(ErrorStatus.REVIEW_DELETE_INVALID);
            }
            DeleteCourseReviewImages(courseReview);
            reviewReportRepository.deleteAllByCourseReview(courseReview);
            courseReviewRepository.delete(courseReview);
        }
        return ReviewReportConverter.toDeleteReviewResponse(reviewId, type);
    }
}
