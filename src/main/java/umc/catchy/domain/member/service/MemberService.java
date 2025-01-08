package umc.catchy.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.member.domain.SocialType;
import umc.catchy.domain.member.dto.request.SignUpRequest;
import umc.catchy.domain.member.dto.response.SignUpResponse;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;

    public SignUpResponse signUp(SignUpRequest request, MultipartFile profileImage, SocialType socialType) {

        // 프로필 이미지 url 생성
        String profileImageUrl = "";

        // providerId 중복 확인
        memberRepository.findByProviderId(request.providerId())
                .ifPresent(member -> {
                    throw new GeneralException(ErrorStatus.PROVIDER_ID_DUPLICATE);
                });

        // 이메일 중복 확인
        memberRepository.findByEmail(request.email())
                .ifPresent(member -> {
                    throw new GeneralException(ErrorStatus.EMAIL_DUPLICATE);
                });

        // 닉네임 중복 확인
        memberRepository.findByNickname(request.nickname())
                .ifPresent(member -> {
                    throw new GeneralException(ErrorStatus.NICKNAME_DUPLICATE);
                });

        Member newMember = createMemberEntity(request, profileImageUrl, socialType);

        memberRepository.save(newMember);

        return SignUpResponse.of(newMember);
    }

    private Member createMemberEntity(SignUpRequest request, String profileImageUrl, SocialType socialType) {
        return Member.createMember(
                request.providerId(),
                request.email(),
                request.nickname(),
                profileImageUrl,
                socialType);
    }
}
