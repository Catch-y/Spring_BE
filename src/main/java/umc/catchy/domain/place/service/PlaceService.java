package umc.catchy.domain.place.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.catchy.domain.category.dao.CategoryRepository;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.category.domain.Category;
import umc.catchy.domain.mapping.placeCourse.dto.query.PlacePreviewDto;
import umc.catchy.domain.mapping.placeCourse.dto.query.PlaceSearchDto;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlacePreviewResponse;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceSearchResponse;
import umc.catchy.domain.mapping.placeVisit.dao.PlaceVisitRepository;
import umc.catchy.domain.mapping.placeVisit.domain.PlaceVisit;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.dao.PlaceRepository;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.domain.place.dto.request.SetCategoryRequest;
import umc.catchy.domain.place.dto.response.RecommendationContext;
import umc.catchy.global.common.dto.SliceResponse;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final CategoryRepository categoryRepository;
    private final MemberRepository memberRepository;
    private final PlaceVisitRepository placeVisitRepository;
    private final PlaceRecommendationService recommendationService;

    @Transactional
    public void setCategories(Long placeId, SetCategoryRequest request) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PLACE_NOT_FOUND));

        BigCategory bigCategory = BigCategory.findByName(request.bigCategory());

        Category category = categoryRepository.findByBigCategoryAndName(bigCategory, request.smallCategory())
                .orElseThrow(() -> new GeneralException(ErrorStatus.INVALID_CATEGORY));

        place.assignCategory(category);
    }

    @Transactional(readOnly = true)
    public SliceResponse<PlacePreviewResponse> recommendPlaces(Double latitude, Double longitude, int pageSize, int page) {
        Member member = getCurrentMember();

        List<PlaceVisit> placeVisits = placeVisitRepository.findAllByMemberWithPlaceAndCategory(member);

        RecommendationContext context = recommendationService.analyzeVisitHistory(placeVisits);

        Slice<PlacePreviewDto> placePreviewDtos = placeRepository.recommendPlacesByActivityData(
                member.getId(),
                latitude,
                longitude,
                context.sortedCategories(),
                context.averageHours(),
                pageSize,
                page
        );

        List<PlacePreviewResponse> response = placePreviewDtos.getContent().stream()
                .map(PlacePreviewResponse::from)
                .toList();

        return new SliceResponse<>(response, placePreviewDtos.isLast());
    }

    @Transactional(readOnly = true)
    public SliceResponse<PlaceSearchResponse> searchPlaceByCategoryOrName(int pageSize, String keyword, Integer lastRelevanceScore, Long lastPlaceId) {
        Slice<PlaceSearchDto> searchDtos = placeRepository.searchPlace(
                pageSize,
                keyword,
                lastRelevanceScore,
                lastPlaceId
        );

        List<PlaceSearchResponse> response = searchDtos.getContent().stream()
                .map(PlaceSearchResponse::from)
                .toList();

        return new SliceResponse<>(response,searchDtos.isLast());
    }

    private Member getCurrentMember() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
    }
}
