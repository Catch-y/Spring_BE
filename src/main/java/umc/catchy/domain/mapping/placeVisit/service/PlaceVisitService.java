package umc.catchy.domain.mapping.placeVisit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.catchy.domain.mapping.placeVisit.dao.PlaceVisitRepository;
import umc.catchy.domain.mapping.placeVisit.domain.PlaceVisit;
import umc.catchy.domain.mapping.placeVisit.dto.response.PlaceLikedResponse;
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

    public void check(Long placeId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PLACE_NOT_FOUND));

        // 올바르지 않은 사용자의 방문 체크라면 예외 처리
        PlaceVisit placeVisit = placeVisitRepository.findByPlaceAndMember(place, member)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PLACE_VISIT_INVALID_MEMBER));

        // 이미 방문 체크되어있다면 예외 처리
        if (placeVisit.isVisited()) {
            throw new GeneralException(ErrorStatus.PLACE_VISIT_ALREADY_CHECK);
        }

        placeVisit.setVisited(true);
    }
}
