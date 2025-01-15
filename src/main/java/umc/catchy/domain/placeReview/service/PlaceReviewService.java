package umc.catchy.domain.placeReview.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class PlaceReviewService {

    private final PlaceRepository placeRepository;
    private final PlaceVisitRepository placeVisitRepository;
    private final PlaceReviewRepository placeReviewRepository;
    private final PlaceReviewImageRepository placeReviewImageRepository;
    private final MemberRepository memberRepository;
    private final AmazonS3Manager amazonS3Manager;

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
        LocalDateTime visitedDate = placeVisitRepository.findByPlaceAndMember(place, member)
                .map(PlaceVisit::getVisitedDate)
                .orElse(null);

        //PlaceReview 엔티티 생성 및 저장
        PlaceReview newPlaceReview = PlaceReviewConverter.toPlaceReview(member, place, request);
        placeReviewRepository.save(newPlaceReview);

        List<PostPlaceReviewResponse.placeReviewImageResponseDTO> reviewImages = new ArrayList<>();
        for(MultipartFile image : request.getImages()){
            //S3에 이미지 업로드
            String keyName = "place-review-images/" + UUID.randomUUID().toString();
            String url = amazonS3Manager.uploadFile(keyName, image);

            //PlaceReviewImage 엔티티 생성 및 저장
            PlaceReviewImage placeReviewImage = PlaceReviewImageConverter.toPlaceReviewImage(newPlaceReview, url);
            placeReviewImageRepository.save(placeReviewImage);
            reviewImages.add(PlaceReviewImageConverter.toPlaceReviewImageResponseDTO(placeReviewImage));
        }
        return PlaceReviewConverter.toNewPlaceReviewResponseDTO(newPlaceReview, reviewImages, visitedDate);
    }
}
