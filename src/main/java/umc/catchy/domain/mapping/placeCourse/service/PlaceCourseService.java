package umc.catchy.domain.mapping.placeCourse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import umc.catchy.domain.mapping.placeCourse.dao.PlaceCourseRepository;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceInfoResponse;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceCourseService {
    private final PlaceCourseRepository placeCourseRepository;
    private final MemberRepository memberRepository;

    public Slice<PlaceInfoResponse> searchLikedPlace(int pageSize, Long lastPlaceId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        return placeCourseRepository.searchPlaceByLiked(memberId, pageSize, lastPlaceId);
    }
}
