package umc.catchy.support.fixture;

import umc.catchy.domain.mapping.placeLike.domain.PlaceLike;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.domain.Place;

public class PlaceLikeFixture {

    public static PlaceLike createPlaceLike(Long id, Member member, Place place, boolean isLiked) {
        return PlaceLike.builder()
                .id(id)
                .member(member)
                .place(place)
                .isLiked(isLiked)
                .build();
    }

    public static PlaceLike createLikedPlace(Member member, Place place) {
        return PlaceLike.builder()
                .id(1L)
                .member(member)
                .place(place)
                .isLiked(true)
                .build();
    }

    public static PlaceLike createUnlikedPlace(Member member, Place place) {
        return PlaceLike.builder()
                .id(1L)
                .member(member)
                .place(place)
                .isLiked(false)
                .build();
    }

    public static PlaceLike createPlaceLike(Member member, Place place) {
        return PlaceLike.builder()
                .id(1L)
                .member(member)
                .place(place)
                .isLiked(false)
                .build();
    }
}
