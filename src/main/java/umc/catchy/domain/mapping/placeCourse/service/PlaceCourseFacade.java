package umc.catchy.domain.mapping.placeCourse.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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
@RequiredArgsConstructor
@Slf4j
public class PlaceCourseFacade {

    private final GooglePlaceClient googlePlaceClient;
    private final MemberRepository memberRepository;
    private final PlaceVisitRepository placeVisitRepository;
    private final PlaceLikeRepository placeLikeRepository;
    private final PlaceRepository placeRepository;
    private final PlaceReviewRepository placeReviewRepository;
    private final PlaceCourseRepository placeCourseRepository;
    private final PlaceCourseCommandService placeCourseCommandService;

    @Qualifier("googlePlaceExecutor")
    private final Executor googlePlaceExecutor;

    public List<PlacePreviewResponse> getPlacesByFrontend(List<PlaceSearchRequest> placeRequests) {
        Member member = getCurrentMember();

        List<Long> poiIds = placeRequests.stream().map(PlaceSearchRequest::poiId).toList();
        Map<Long, Place> existingPlaceMap = placeRepository.findAllByPoiIdIn(poiIds).stream()
                .collect(Collectors.toMap(Place::getPoiId, p -> p));

        List<CompletableFuture<Place>> futures = placeRequests.stream()
                .map(req -> {
                    Place p = existingPlaceMap.get(req.poiId());
                    if (p != null) return CompletableFuture.completedFuture(p);

                    // 개별 비동기 작업에 handle을 추가하여 예외 발생 시 null을 반환하도록 처리
                    return CompletableFuture.supplyAsync(() -> createPlaceFromGoogle(req), googlePlaceExecutor)
                            .handle((result, ex) -> {
                                if (ex != null) {
                                    log.error("장소 생성 실패 (POI ID: {}): {}", req.poiId(), ex.getMessage());
                                    return null;
                                }
                                return result;
                            });
                }).toList();

        List<Place> allPlaces = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull) // 실패한(null) 장소는 결과 리스트에서 제외
                .toList();

        List<Long> dbIds = allPlaces.stream().map(Place::getId).toList();
        Map<Long, Long> reviewCountMap = placeReviewRepository.countReviewByPlaceIds(dbIds);
        Set<Long> likedPlaceIds = placeLikeRepository.findLikedPlaceIdsByMemberAndPlaceIds(member.getId(), dbIds);

        return allPlaces.stream()
                .map(place -> PlacePreviewResponse.from(
                        place,
                        reviewCountMap.getOrDefault(place.getId(), 0L),
                        likedPlaceIds.contains(place.getId())
                )).toList();
    }

    @Transactional(readOnly = true)
    public PlaceDetailResponse getPlaceDetailByPlaceId(Long placeId) {
        Member member = getCurrentMember();

        Place place = placeRepository.findByIdWithCategory(placeId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PLACE_NOT_FOUND));

        Long reviewCount = placeReviewRepository.countByPlaceId(placeId);

        Boolean isVisited = placeVisitRepository.findByPlaceAndMember(place, member)
                .map(PlaceVisit::isVisited).orElse(false);

        Boolean isLiked = placeLikeRepository.findByPlaceAndMember(place, member).isPresent();

        return PlaceDetailResponse.from(place, reviewCount, isVisited, isLiked);
    }

    @Transactional(readOnly = true)
    public SliceResponse<PlaceResponse> searchLikedPlace(int pageSize, Long lastPlaceId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Slice<PlaceDto> placeDtos = placeCourseRepository.searchPlaceByLiked(memberId, pageSize, lastPlaceId);

        List<PlaceResponse> responses = placeDtos.getContent().stream()
                .map(PlaceResponse::from)
                .toList();

        return new SliceResponse<>(responses, placeDtos.isLast());
    }

    private Place createPlaceFromGoogle(PlaceSearchRequest request) {
        try {
            Map<String, String> placeDetails = googlePlaceClient.getPlaceInfo(
                    request.placeName(),
                    request.address(),
                    request.latitude(),
                    request.longitude()
            );

            return placeCourseCommandService.savePlaceFromGoogleInfo(request.poiId(), placeDetails);
        } catch (Exception e) {
            log.error("Google API processing failed: poiId={}, name={}", request.poiId(), request.placeName(), e);
            throw new GeneralException(ErrorStatus.SEARCH_PLACE_NOT_FOUND);
        }
    }

    private Member getCurrentMember() {
        return memberRepository.findById(SecurityUtil.getCurrentMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
    }
}
