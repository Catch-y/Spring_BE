package umc.catchy.domain.mapping.placeCourse.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.catchy.domain.mapping.placeCourse.dao.PlaceCourseRepository;
import umc.catchy.domain.mapping.placeCourse.dto.query.PlaceDto;
import umc.catchy.domain.mapping.placeCourse.dto.request.PlaceSearchRequest;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceDetailResponse;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlacePreviewResponse;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceResponse;
import umc.catchy.domain.mapping.placeLike.dao.PlaceLikeRepository;
import umc.catchy.domain.mapping.placeLike.domain.PlaceLike;
import umc.catchy.domain.mapping.placeVisit.dao.PlaceVisitRepository;
import umc.catchy.domain.mapping.placeVisit.domain.PlaceVisit;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.dao.PlaceRepository;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.domain.placeReview.dao.PlaceReviewRepository;
import umc.catchy.global.common.dto.SliceResponse;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;
import umc.catchy.infra.google.GooglePlaceClient;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PlaceCourseService {

    private final GooglePlaceClient googlePlaceClient;
    private final MemberRepository memberRepository;
    private final PlaceVisitRepository placeVisitRepository;
    private final PlaceLikeRepository placeLikeRepository;
    private final PlaceRepository placeRepository;
    private final PlaceReviewRepository placeReviewRepository;
    private final PlaceCourseRepository placeCourseRepository;

    public List<PlacePreviewResponse> getPlacesByFrontend(
            List<PlaceSearchRequest> placeRequests
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        return placeRequests.stream()
                .map(request -> processPlaceRequest(request, member))
                .toList();
    }

    public PlaceDetailResponse getPlaceDetailByPlaceId(Long placeId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        Place place = placeRepository.findByIdWithCategory(placeId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PLACE_NOT_FOUND));

        Long reviewCount = placeReviewRepository.countByPlaceId(placeId);

        Optional<PlaceVisit> placeVisit = placeVisitRepository.findByPlaceAndMember(place, member);
        Boolean isVisited = placeVisit.map(PlaceVisit::isVisited).orElse(false);

        Optional<PlaceLike> placeLike = placeLikeRepository.findByPlaceAndMember(place, member);
        Boolean isLiked = placeLike.map(PlaceLike::isLiked).orElse(false);

        return PlaceDetailResponse.from(place, reviewCount, isVisited, isLiked);
    }

    public SliceResponse<PlaceResponse> searchLikedPlace(int pageSize, Long lastPlaceId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Slice<PlaceDto> placeDtos = placeCourseRepository.searchPlaceByLiked(memberId, pageSize, lastPlaceId);

        List<PlaceResponse> responses = placeDtos.getContent()
                .stream()
                .map(PlaceResponse::from)
                .toList();

        return new SliceResponse<>(responses, placeDtos.isLast());
    }

    private PlacePreviewResponse processPlaceRequest(PlaceSearchRequest request, Member member) {
        try {
            // 1. DB에서 조회
            Optional<Place> existingPlace = placeRepository.findByPoiId(request.poiId());

            if (existingPlace.isPresent()) {
                // DB에 있음 → 기존 로직
                Place place = existingPlace.get();
                Long reviewCount = placeReviewRepository.countByPlaceId(place.getId());
                Boolean isLiked = placeLikeRepository.findByPlaceAndMember(place, member).isPresent();
                return PlacePreviewResponse.from(place, reviewCount, isLiked);
            } else {
                // DB에 없음 → Google API 호출
                return createPlaceFromGoogle(request, member);
            }

        } catch (Exception e) {
            log.error("장소 처리 실패: poiId={}, placeName={}", request.poiId(), request.placeName(), e);
            throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
        }
    }

    private PlacePreviewResponse createPlaceFromGoogle(PlaceSearchRequest request, Member member) {
        try {
            // 1. Google Place ID 찾기
            String googlePlaceId = googlePlaceClient.findPlaceId(
                    request.placeName(),
                    request.address(),
                    request.latitude(),
                    request.longitude()
            );

            // 2. 장소 상세 정보 가져오기
            Map<String, String> placeDetails = googlePlaceClient.getPlaceDetails(googlePlaceId);

            // 3. Place 엔티티 생성 및 저장
            Place place = Place.fromGoogleInfo(request.poiId(), placeDetails);
            placeRepository.save(place);

            // 4. Response 생성
            return PlacePreviewResponse.from(place, 0L, false);

        } catch (Exception e) {
            log.error("Google API를 통한 장소 생성 실패: {}", request, e);
            throw new GeneralException(ErrorStatus.SEARCH_PLACE_NOT_FOUND);
        }
    }
}
