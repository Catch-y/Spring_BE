package umc.catchy.domain.member.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.member.dto.response.NicknameResponse;
import umc.catchy.domain.member.dto.response.ProfileImageResponse;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberProfileCommandService {

    private final MemberRepository memberRepository;

    public NicknameResponse updateNickname(Long memberId, String nickname) {
        Member member = findMember(memberId);
        member.updateNickname(nickname);
        return NicknameResponse.of(member);
    }

    public ProfileImageResponse updateProfileImage(Long memberId, String newUrl) {
        Member member = findMember(memberId);
        member.updateProfileImage(newUrl);
        return ProfileImageResponse.of(member);
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
    }
}
