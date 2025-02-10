package umc.catchy.domain.place.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.catchy.domain.category.dao.CategoryRepository;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.category.domain.Category;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceInfoPreview;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceInfoPreviewSliceResponse;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceInfoResponse;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceInfoSliceResponse;
import umc.catchy.domain.mapping.placeVisit.dao.PlaceVisitRepository;
import umc.catchy.domain.mapping.placeVisit.domain.PlaceVisit;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.dao.PlaceRepository;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.domain.place.dto.request.SetCategoryRequest;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;

@Service
@RequiredArgsConstructor
@Transactional
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final CategoryRepository categoryRepository;
    private final MemberRepository memberRepository;
    private final PlaceVisitRepository placeVisitRepository;

    // 장소 카테고리 선택
    public void setCategories(Long placeId, SetCategoryRequest request) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PLACE_NOT_FOUND));

        // 이미 지정된 카테고리가 있으면 예외 처리
        if (place.getCategory() != null) {
            throw new GeneralException(ErrorStatus.PLACE_CATEGORY_EXIST);
        }

        // 대카테고리 검증
        BigCategory bigCategory = BigCategory.findByName(request.getBigCategory());

        Category category = categoryRepository.findByBigCategoryAndName(bigCategory, request.getSmallCategory())
                .orElseThrow(() -> new GeneralException(ErrorStatus.INVALID_CATEGORY));

        // 장소에 카테고리 설정
        place.setCategory(category);
    }

    // 사용자 맞춤 장소 추천 37.5837064 127.21166595
    public PlaceInfoPreviewSliceResponse recommendPlaces(Double latitude, Double longitude, int pageSize, int page) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        // 최근 방문했던 장소를 기반으로 추천
        List<PlaceVisit> placeVisits = placeVisitRepository.findAllByMemberOrderByVisitedDateDesc(member);

        // 방문 카테고리가 많은 순으로 정렬
        List<Long> sortedVisitCategories = sortVisitCategories(getVisitCategories(placeVisits));

        // 카테고리별 방문 시간대 평균
        Map<Long, Integer> categoryAverageHour = getCategoryAverageHour(placeVisits);

        Slice<PlaceInfoPreview> placeInfoPreviews = placeRepository.recommendPlacesByActivityData(memberId, latitude, longitude, sortedVisitCategories, categoryAverageHour, pageSize, page);

        return PlaceInfoPreviewSliceResponse.from(placeInfoPreviews);
    }

    public PlaceInfoSliceResponse searchPlaceByCategoryOrName(int pageSize, String keyword, Long lastPlaceId) {
        Slice<PlaceInfoResponse> responses = placeRepository.searchPlace(pageSize, keyword, lastPlaceId);
        return PlaceInfoSliceResponse.from(responses);
    }

    private Map<Long, Integer> getCategoryAverageHour(List<PlaceVisit> placeVisits) {
        Map<Long, Integer> categoryAverageHour = new HashMap<>();
        Map<Long, List<LocalDateTime>> categoryVisitTimes = new HashMap<>();

        for (PlaceVisit visit : placeVisits) {
            Category category = visit.getPlace().getCategory();
            LocalDateTime visitedTime = visit.getCreatedDate();

            // 만약 Map에 존재하지 않는 key면, List를 생성하고 값을 삽입
            categoryVisitTimes.computeIfAbsent(category.getId(), k -> new ArrayList<>()).add(visitedTime);
        }

        for (Map.Entry<Long, List<LocalDateTime>> entry : categoryVisitTimes.entrySet()) {
            Long categoryId = entry.getKey();
            List<LocalDateTime> visitTimes = entry.getValue();

            // 시간대 평균 계산
            int averageHour = (int) Math.round(visitTimes.stream()
                    .mapToDouble(LocalDateTime::getHour)
                    .average()
                    .orElse(0));

            categoryAverageHour.put(categoryId, averageHour);
        }

        return categoryAverageHour;
    }

    private List<Category> getVisitCategories(List<PlaceVisit> placeVisits) {
        return placeVisits.stream()
                .map(PlaceVisit::getPlace)
                .map(Place::getCategory)
                .toList();
    }

    private List<Long> sortVisitCategories(List<Category> visitCategories) {
        Map<Category, Integer> categoryMap = new HashMap<>();

        visitCategories.forEach(category -> categoryMap.put(category, categoryMap.getOrDefault(category, 0) + 1));
        List<Entry<Category, Integer>> entries = new ArrayList<>(categoryMap.entrySet());

        entries.sort((o1, o2) -> o2.getValue() - o1.getValue());

        return entries.stream().map(entry -> entry.getKey().getId()).toList();
    }
}
