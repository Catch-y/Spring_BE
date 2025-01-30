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
import umc.catchy.domain.placeReview.converter.PlaceReviewConverter;
import umc.catchy.domain.placeReview.dao.PlaceReviewRepository;
import umc.catchy.domain.placeReview.domain.PlaceReview;
import umc.catchy.domain.placeReview.dto.request.PostPlaceReviewRequest;
import umc.catchy.domain.placeReview.dto.response.PostPlaceReviewResponse;
import umc.catchy.domain.placeReviewImage.converter.PlaceReviewImageConverter;
import umc.catchy.domain.placeReviewImage.dao.PlaceReviewImageRepository;
import umc.catchy.domain.placeReviewImage.domain.PlaceReviewImage;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
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
        course.setRating(rating);
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
            place.setRating(averageRating);
            placeRepository.save(place);
        }
        //장소를 포함하는 코스에 대한 평점 refresh
        placeCourseRepository.findAllByPlace(place)
                .forEach(placeCourse -> {
                    refreshCourseRating(placeCourse.getCourse());
                });
    }

    public PostPlaceReviewResponse.newPlaceReviewResponseDTO postNewPlaceReview(PostPlaceReviewRequest request, Long placeId){
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

        //장소 방문일자 가져오기
        LocalDate visitedDate = placeVisitRepository.findByPlaceAndMember(place, member)
                .map(PlaceVisit::getVisitedDate)
                .orElse(null);

        //PlaceReview 엔티티 생성 및 저장
        PlaceReview newPlaceReview = PlaceReviewConverter.toPlaceReview(member, place, request);
        placeReviewRepository.save(newPlaceReview);
        //Place::rating refresh
        refreshPlaceRating(newPlaceReview.getPlace());

        List<PostPlaceReviewResponse.placeReviewImageResponseDTO> reviewImages = new ArrayList<>();
        for(MultipartFile image : request.getImages()){
            //S3에 이미지 업로드
            String keyName = "review/place-review-images/" + UUID.randomUUID().toString();
            String url = amazonS3Manager.uploadFile(keyName, image);

            //PlaceReviewImage 엔티티 생성 및 저장
            PlaceReviewImage placeReviewImage = PlaceReviewImageConverter.toPlaceReviewImage(newPlaceReview, url);
            placeReviewImageRepository.save(placeReviewImage);
            reviewImages.add(PlaceReviewImageConverter.toPlaceReviewImageResponseDTO(placeReviewImage));
        }
        return PlaceReviewConverter.toNewPlaceReviewResponseDTO(newPlaceReview, reviewImages, visitedDate);
    }

    public PostPlaceReviewResponse.placeReviewAllResponseDTO getAllPlaceReviews(Long placeId, int pageSize, Long lastPlaceReviewId) {
        Float averageRating = placeReviewRepository.findAverageRatingByPlaceId(placeId);
        List<PostPlaceReviewResponse.placeReviewRatingResponseDTO> ratingList = placeReviewRepository.findRatingList(placeId);
        Long totalCount = placeReviewRepository.countByPlaceId(placeId);
        Slice<PostPlaceReviewResponse.newPlaceReviewResponseDTO> contentList = placeReviewRepository.findPlaceReviewSliceByPlaceId(placeId, pageSize, lastPlaceReviewId);
        List<PostPlaceReviewResponse.newPlaceReviewResponseDTO> content = contentList.getContent();
        Boolean last = contentList.isLast();

        return PostPlaceReviewResponse.placeReviewAllResponseDTO.builder()
                .averageRating(averageRating)
                .ratingList(ratingList)
                .totalCount(totalCount)
                .content(content)
                .last(last)
                .build();
    }
}
