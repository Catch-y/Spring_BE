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
}
