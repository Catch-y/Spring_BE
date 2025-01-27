package umc.catchy.domain.place.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.catchy.domain.category.dao.CategoryRepository;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.category.domain.Category;
import umc.catchy.domain.place.dao.PlaceRepository;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.domain.place.dto.request.SetCategoryRequest;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;

@Service
@RequiredArgsConstructor
@Transactional
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final CategoryRepository categoryRepository;

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

        // 카테고리 생성
        Category category = Category.builder()
                .bigCategory(bigCategory)
                .name(request.getSmallCategory())
                .build();

        categoryRepository.save(category);

        // 장소에 카테고리 설정
        place.setCategory(category);
    }

}
