package umc.catchy.domain.placeReview.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import umc.catchy.domain.course.dao.CourseRepository;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.mapping.placeCourse.dao.PlaceCourseRepository;
import umc.catchy.domain.mapping.placeVisit.dao.PlaceVisitRepository;
import umc.catchy.domain.mapping.placeVisit.domain.PlaceVisit;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.dao.PlaceRepository;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.domain.placeReview.dao.PlaceReviewRepository;
import umc.catchy.domain.placeReview.domain.PlaceReview;
import umc.catchy.domain.placeReview.dto.request.PostPlaceReviewRequest;
import umc.catchy.domain.placeReview.dto.response.PlaceReviewImageResponse;
import umc.catchy.domain.placeReview.dto.response.PlaceReviewListResponse;
import umc.catchy.domain.placeReview.dto.response.PlaceReviewRatingResponse;
import umc.catchy.domain.placeReview.dto.response.PlaceReviewResponse;
import umc.catchy.domain.placeReviewImage.dao.PlaceReviewImageRepository;
import umc.catchy.domain.placeReviewImage.domain.PlaceReviewImage;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.error.exception.ResultEmptyListException;
import umc.catchy.global.util.SecurityUtil;
import umc.catchy.infra.aws.s3.AmazonS3Manager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class PlaceReviewService {

    private final PlaceRepository placeRepository;
    private final PlaceCourseRepository placeCourseRepository;
    private final PlaceVisitRepository placeVisitRepository;
    private final PlaceReviewRepository placeReviewRepository;
    private final PlaceReviewImageRepository placeReviewImageRepository;
    private final CourseRepository courseRepository;
    private final MemberRepository memberRepository;
    private final AmazonS3Manager amazonS3Manager;

    //코스 평점 계산
    private void refreshCourseRating(Course course){
        Double rating = placeCourseRepository.calculateAverageRatingByCourse(course);
        rating = (rating != null) ? rating : 0.0;
        course.updateRating(rating);
        courseRepository.save(course);
    }

    //장소 rating refresh
    private void refreshPlaceRating(Place place){
        List<PlaceReview> reviews = placeReviewRepository.findAllByPlace(place);
        if(!reviews.isEmpty()){
            double averageRating = reviews.stream()
                    .mapToDouble(PlaceReview::getRating)
                    .average()
                    .orElse(0.0);
            place.updateRating(averageRating);
            placeRepository.save(place);
        }
        //장소를 포함하는 코스에 대한 평점 refresh
        placeCourseRepository.findAllByPlace(place)
                .forEach(placeCourse -> {
                    refreshCourseRating(placeCourse.getCourse());
                });
    }

    public PlaceReviewResponse postNewPlaceReview(PostPlaceReviewRequest request, Long placeId){
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        Place place = placeRepository.findById(placeId)
                .orElseThrow(()-> new GeneralException(ErrorStatus.PLACE_NOT_FOUND));

        //멤버의 장소 방문 여부 확인
        Boolean isVisited = placeVisitRepository.findByPlaceAndMember(place, member)
                .map(PlaceVisit::isVisited)
                .orElse(false);
        if(!isVisited){
            throw new GeneralException(ErrorStatus.PLACE_REVIEW_INVALID_MEMBER);
        }

        //PlaceReview 엔티티 생성 및 저장 (정적 팩토리 메서드 사용)
        PlaceReview newPlaceReview = PlaceReview.create(
                member,
                place,
                request.rating(),
                request.comment(),
                request.visitedDate()
        );
        placeReviewRepository.save(newPlaceReview);

        //Place::rating refresh
        refreshPlaceRating(newPlaceReview.getPlace());

        List<PlaceReviewImageResponse> reviewImages = new ArrayList<>();
        for(MultipartFile image : request.images()){
            //S3에 이미지 업로드
            String keyName = "review/place-review-images/" + UUID.randomUUID().toString();
            String url = amazonS3Manager.uploadFile(keyName, image);

            //PlaceReviewImage 엔티티 생성 및 저장
            PlaceReviewImage placeReviewImage = PlaceReviewImage.create(url, newPlaceReview);
            placeReviewImageRepository.save(placeReviewImage);
            reviewImages.add(PlaceReviewImageResponse.from(placeReviewImage));
        }

        return PlaceReviewResponse.from(newPlaceReview, reviewImages);
    }

    @Transactional(readOnly = true)
    public PlaceReviewListResponse getAllPlaceReviews(Long placeId, int pageSize, LocalDate lastPlaceReviewDate, Long lastPlaceReviewId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PLACE_NOT_FOUND));

        Double averageRatingTypeDouble = placeReviewRepository.findAverageRatingByPlaceId(placeId)
                .orElseThrow(() -> new ResultEmptyListException(ErrorStatus.PLACE_REVIEW_NOT_FOUND));
        Float averageRating = Math.round(averageRatingTypeDouble * 10) / 10.0f;

        List<PlaceReviewRatingResponse> ratingList = placeReviewRepository.findRatingList(placeId);
        Long totalCount = placeReviewRepository.countByPlaceId(placeId);
        Slice<PlaceReviewResponse> contentList = placeReviewRepository.findPlaceReviewSliceByPlaceId(placeId, pageSize, lastPlaceReviewDate, lastPlaceReviewId);

        return new PlaceReviewListResponse(
                averageRating,
                ratingList,
                totalCount,
                contentList.getContent(),
                contentList.isLast()
        );
    }
}
