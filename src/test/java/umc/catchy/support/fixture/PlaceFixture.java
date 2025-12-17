package umc.catchy.support.fixture;

import umc.catchy.domain.place.domain.Place;

public class PlaceFixture {

    public static Place createPlace(Long id, double rating) {
        return Place.builder()
                .id(id)
                .placeName("테스트 장소 " + id)
                .rating(rating)
                .build();
    }
}
