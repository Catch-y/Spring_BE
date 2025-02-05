package umc.catchy.domain.mapping.placeLike.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.catchy.domain.mapping.placeLike.converter.PlaceLikeConverter;
import umc.catchy.domain.mapping.placeLike.dao.PlaceLikeRepository;
import umc.catchy.domain.mapping.placeLike.domain.PlaceLike;
import umc.catchy.domain.mapping.placeLike.dto.response.PlaceLikedResponse;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.dao.PlaceRepository;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PlaceLikeService {
    private final PlaceLikeRepository placeLikeRepository;
    private final MemberRepository memberRepository;
    private final PlaceRepository placeRepository;

    public PlaceLikedResponse togglePlaceLiked(Long placeId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PLACE_NOT_FOUND));

        Optional<PlaceLike> optionalPlaceLike = placeLikeRepository.findByPlaceAndMember(place, member);
        PlaceLike placeLike = optionalPlaceLike.orElseGet(() -> PlaceLikeConverter.toPlaceLike(place, member));

        if (optionalPlaceLike.isEmpty()) placeLikeRepository.save(placeLike);

        PlaceLike.toggleLiked(placeLike);

        return PlaceLikeConverter.toPlaceLikedResponse(placeLike);
    }
}
