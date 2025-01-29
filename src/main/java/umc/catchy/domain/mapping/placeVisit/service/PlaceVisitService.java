package umc.catchy.domain.mapping.placeVisit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.catchy.domain.mapping.placeVisit.converter.PlaceVisitConverter;
import umc.catchy.domain.mapping.placeVisit.dao.PlaceVisitRepository;
import umc.catchy.domain.mapping.placeVisit.domain.PlaceVisit;
import umc.catchy.domain.mapping.placeVisit.dto.response.PlaceLikedResponse;
import umc.catchy.domain.mapping.placeVisit.dto.response.PlaceVisitedResponse;
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
public class PlaceVisitService {
    private final PlaceVisitRepository placeVisitRepository;
    private final MemberRepository memberRepository;
    private final PlaceRepository placeRepository;

    public PlaceLikedResponse togglePlaceLiked(Long placeId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member currentMember = memberRepository.findById(memberId).orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
        PlaceVisit placeVisit = placeVisitRepository.findByPlaceIdAndMemberId(placeId,currentMember.getId()).orElseThrow(() -> new GeneralException(ErrorStatus.INVALID_PARAMETER));
        PlaceVisit.toggleLiked(placeVisit);
        return PlaceLikedResponse.builder()
                .placeVisitId(placeVisit.getId())
                .liked(placeVisit.isLiked())
                .build();
    }

    public PlaceVisitedResponse check(Long placeId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PLACE_NOT_FOUND));

        // placeVisit 생성
        PlaceVisit placeVisit = PlaceVisitConverter.toPlaceVisit(place, member);

        placeVisitRepository.save(placeVisit);

        return PlaceVisitConverter.toPlaceVisitResponse(placeVisit);
    }
}
