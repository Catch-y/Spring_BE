package umc.catchy.support.fixture;

import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.category.domain.Category;

public class CategoryFixture {

    public static Category createCategory(Long id, String name, BigCategory bigCategory) {
        return Category.builder()
                .id(id)
                .name(name)
                .bigCategory(bigCategory)
                .build();
    }

    public static Category createCafeCategory() {
        return Category.builder()
                .id(1L)
                .name("카페")
                .bigCategory(BigCategory.CAFE)
                .build();
    }

    public static Category createRestaurantCategory() {
        return Category.builder()
                .id(2L)
                .name("한식")
                .bigCategory(BigCategory.RESTAURANT)
                .build();
    }

    public static Category createCultureCategory() {
        return Category.builder()
                .id(3L)
                .name("미술관")
                .bigCategory(BigCategory.CULTURELIFE)
                .build();
    }
}
