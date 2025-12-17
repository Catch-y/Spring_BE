package umc.catchy.support.fixture;

import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.category.domain.Category;
import umc.catchy.domain.place.domain.Place;

public class PlaceFixture {

    public static Place createPlace(Long id, double rating) {
        Category category = Category.builder()
                .id(1L)
                .name("테스트 카테고리")
                .bigCategory(BigCategory.CAFE)
                .build();

        return Place.builder()
                .id(id)
                .placeName("테스트 장소 " + id)
                .rating(rating)
                .category(category)
                .latitude(37.5665)
                .longitude(126.9780)
                .build();
    }
}
