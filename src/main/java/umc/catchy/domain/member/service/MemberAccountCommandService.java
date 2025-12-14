package umc.catchy.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.member.service.OAuthService.SocialUserInfo;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberAccountCommandService {

    private final MemberRepository memberRepository;

    public Member saveMember(Member member) {
        return memberRepository.save(member);
    }

    public void deleteMember(Long memberId) {
        Member member = findMemberById(memberId);
        memberRepository.deleteAllRelatedEntities(memberId);
        memberRepository.delete(member);
    }

    public void toggleAppAlarm(Long memberId) {
        Member member = findMemberById(memberId);
        member.toggleAppAlarmState(member.getFcmInfo());
    }

    public void updateFcmToken(Long memberId, String token) {
        Member member = findMemberById(memberId);
        member.updateFcmToken(member.getFcmInfo(), token);
    }

    @Transactional(readOnly = true)
    public Member findMemberBySocialInfo(SocialUserInfo socialUserInfo) {
        return memberRepository.findByEmailAndProviderId(
                socialUserInfo.email(), socialUserInfo.providerId()
        ).orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public void validateDuplicates(SocialUserInfo socialUserInfo, String nickname) {
        memberRepository.findByProviderId(socialUserInfo.providerId())
                .ifPresent(member -> { throw new GeneralException(ErrorStatus.PROVIDER_ID_DUPLICATE); });

        memberRepository.findByEmail(socialUserInfo.email())
                .ifPresent(member -> { throw new GeneralException(ErrorStatus.EMAIL_DUPLICATE); });

        memberRepository.findByNickname(nickname)
                .ifPresent(member -> { throw new GeneralException(ErrorStatus.NICKNAME_DUPLICATE); });
    }
}
