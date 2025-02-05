package umc.catchy.domain.mapping.placeLike.converter;

import umc.catchy.domain.mapping.placeLike.domain.PlaceLike;
import umc.catchy.domain.mapping.placeLike.dto.response.PlaceLikedResponse;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.domain.Place;

public class PlaceLikeConverter {
    public static PlaceLike toPlaceLike(Place place, Member member) {
        return PlaceLike.builder()
                .member(member)
                .place(place)
                .isLiked(false)
                .build();
    }

    public static PlaceLikedResponse toPlaceLikedResponse(PlaceLike placeLike) {
        return PlaceLikedResponse.builder()
                .placeLikeId(placeLike.getId())
                .isLiked(placeLike.isLiked())
                .build();
    }
}
