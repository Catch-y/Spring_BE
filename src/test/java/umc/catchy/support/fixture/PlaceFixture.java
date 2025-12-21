package umc.catchy.support.fixture;

import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.category.domain.Category;
import umc.catchy.domain.place.domain.Place;

import java.time.LocalTime;

public class PlaceFixture {

    public static Place createPlace(Long id, double rating) {
        Category category = Category.builder()
                .id(1L)
                .name("카페")
                .bigCategory(BigCategory.CAFE)
                .build();

        return Place.builder()
                .id(id)
                .poiId(1000L + id)
                .placeName("테스트 장소 " + id)
                .placeDescription("테스트 설명")
                .roadAddress("서울특별시 강남구 테스트로 " + id)
                .rating(rating)
                .category(category)
                .latitude(37.5665)
                .longitude(126.9780)
                .imageUrl("https://s3.aws.com/place" + id + ".jpg")
                .activeTime("매일 · 09:00 - 22:00")
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(22, 0))
                .build();
    }

    public static Place createPlaceWithCategory(Long id, Category category) {
        return Place.builder()
                .id(id)
                .poiId(1000L + id)
                .placeName("테스트 장소 " + id)
                .placeDescription("테스트 설명")
                .roadAddress("서울특별시 강남구 테스트로 " + id)
                .rating(4.5)
                .category(category)
                .latitude(37.5665)
                .longitude(126.9780)
                .imageUrl("https://s3.aws.com/place" + id + ".jpg")
                .activeTime("매일 · 09:00 - 22:00")
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(22, 0))
                .placeSite("https://place-site.com")
                .build();
    }

    public static Place createPlaceWithLocation(Long id, double latitude, double longitude) {
        Category category = Category.builder()
                .id(1L)
                .name("카페")
                .bigCategory(BigCategory.CAFE)
                .build();

        return Place.builder()
                .id(id)
                .poiId(1000L + id)
                .placeName("테스트 장소 " + id)
                .placeDescription("테스트 설명")
                .roadAddress("서울특별시 강남구 테스트로 " + id)
                .rating(4.5)
                .category(category)
                .latitude(latitude)
                .longitude(longitude)
                .imageUrl("https://s3.aws.com/place" + id + ".jpg")
                .activeTime("매일 · 09:00 - 22:00")
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(22, 0))
                .build();
    }

    public static Place createPlaceWithoutCategory(Long id) {
        return Place.builder()
                .id(id)
                .poiId(1000L + id)
                .placeName("카테고리 없는 장소 " + id)
                .placeDescription("테스트 설명")
                .roadAddress("서울특별시 강남구 테스트로 " + id)
                .rating(4.5)
                .category(null)
                .latitude(37.5665)
                .longitude(126.9780)
                .imageUrl("https://s3.aws.com/place" + id + ".jpg")
                .activeTime("매일 · 09:00 - 22:00")
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(22, 0))
                .build();
    }

    public static Place createPlaceWithPoiId(Long id, Long poiId) {
        Category category = Category.builder()
                .id(1L)
                .name("카페")
                .bigCategory(BigCategory.CAFE)
                .build();

        return Place.builder()
                .id(id)
                .poiId(poiId)
                .placeName("테스트 장소 " + id)
                .placeDescription("테스트 설명")
                .roadAddress("서울특별시 강남구 테스트로 " + id)
                .rating(4.5)
                .category(category)
                .latitude(37.5665)
                .longitude(126.9780)
                .imageUrl("https://s3.aws.com/place" + id + ".jpg")
                .activeTime("매일 · 09:00 - 22:00")
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(22, 0))
                .build();
    }
}
