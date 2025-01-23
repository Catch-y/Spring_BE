package umc.catchy.domain.courseReview.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import umc.catchy.domain.course.dao.CourseRepository;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.courseReview.converter.CourseReviewConverter;
import umc.catchy.domain.courseReview.dao.CourseReviewRepository;
import umc.catchy.domain.courseReview.domain.CourseReview;
import umc.catchy.domain.courseReview.dto.request.PostCourseReviewRequest;
import umc.catchy.domain.courseReview.dto.response.CourseReviewSliceResponse;
import umc.catchy.domain.courseReview.dto.response.PostCourseReviewResponse;
import umc.catchy.domain.courseReviewImage.converter.CourseReviewImageConverter;
import umc.catchy.domain.courseReviewImage.dao.CourseReviewImageRepository;
import umc.catchy.domain.courseReviewImage.domain.CourseReviewImage;
import umc.catchy.domain.mapping.memberCourse.dao.MemberCourseRepository;
import umc.catchy.domain.mapping.memberCourse.domain.MemberCourse;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;
import umc.catchy.infra.aws.s3.AmazonS3Manager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CourseReviewService {

    private final AmazonS3Manager amazonS3Manager;
    private final MemberRepository memberRepository;
    private final MemberCourseRepository memberCourseRepository;
    private final CourseRepository courseRepository;
    private final CourseReviewRepository courseReviewRepository;
    private final CourseReviewImageRepository courseReviewImageRepository;

    public PostCourseReviewResponse.newCourseReviewResponseDTO postNewCourseReview(Long courseId, PostCourseReviewRequest request){
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COURSE_NOT_FOUND));

        //멤버의 코스 참여 여부 확인
        Boolean isVisited = memberCourseRepository.findByCourseAndMember(course, member)
                .map(MemberCourse::isVisited)
                .orElse(false);
        if(!isVisited){
            throw new GeneralException(ErrorStatus.COURSE_REVIEW_INVALID_MEMBER);
        }

        //코스 참여일자 가져오기
        LocalDateTime visitedDate = memberCourseRepository.findByCourseAndMember(course, member)
                .map(MemberCourse::getVisitedDate)
                .orElse(null);

        //CourseReview Entity 생성 및 저장
        CourseReview newCourseReview = CourseReviewConverter.toCourseReview(member, course, request);
        courseReviewRepository.save(newCourseReview);
        //Course::hasReview refresh
        if(!course.isHasReview()){
            course.setHasReview(true);
            courseRepository.save(course);
        }

        List<PostCourseReviewResponse.courseReviewImageResponseDTO> images = new ArrayList<>();
        for(MultipartFile image : request.getImages()){
            //S3에 이미지 업로드
            String keyName = "review/course-review-images/" + UUID.randomUUID().toString();
            String url = amazonS3Manager.uploadFile(keyName, image);

            //CourseReview Entity 생성 및 저장
            CourseReviewImage courseReviewImage = CourseReviewImageConverter.toCourseReviewImage(url, newCourseReview);
            courseReviewImageRepository.save(courseReviewImage);
            images.add(CourseReviewImageConverter.toCourseReviewImageResponseDTO(courseReviewImage));
        }
        return CourseReviewConverter.toNewCourseReviewResponseDTO(newCourseReview, images, visitedDate);
    }

    @Transactional(readOnly = true)
    public PostCourseReviewResponse.courseReviewAllResponseDTO searchAllReview(Long courseId, int pageSize, Long lastReviewId ) {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new GeneralException(ErrorStatus.COURSE_NOT_FOUND));
        Integer countReviews = courseReviewRepository.countAllByCourse(course);
        Slice<PostCourseReviewResponse.newCourseReviewResponseDTO> CourseReviewResponses = courseReviewRepository.searchAllReviewByCourseId(courseId, pageSize, lastReviewId);
        CourseReviewSliceResponse CourseReviews = CourseReviewSliceResponse.from(CourseReviewResponses);
        return PostCourseReviewResponse.courseReviewAllResponseDTO.builder()
                .courseRating(course.getRating())
                .totalCount(countReviews)
                .courseReviewSliceResponse(CourseReviews)
                .build();
    }
}
