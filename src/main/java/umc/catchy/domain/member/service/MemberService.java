package umc.catchy.domain.member.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.member.domain.SocialType;
import umc.catchy.domain.member.dto.request.LoginRequest;
import umc.catchy.domain.member.dto.request.SignUpRequest;
import umc.catchy.domain.member.dto.response.LoginResponse;
import umc.catchy.domain.member.dto.response.SignUpResponse;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.JWTUtil;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final JWTUtil jwtUtil;

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

    public LoginResponse login(LoginRequest request) {
        String email = request.email();
        Long providerId = request.providerId();

        // 멤버 조회
        Optional<Member> optionalMember = memberRepository.findByEmailAndProviderId(email, providerId);

        // 계정이 존재하지 않을 경우
        Member member = optionalMember.orElseThrow(() ->
                new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        // 로그인 성공 시 토큰 생성
        String accessToken = jwtUtil.createAccessToken(email);
        String refreshToken = jwtUtil.createRefreshToken(email);

        member.setRefresh_token(accessToken);
        member.setRefresh_token(refreshToken);

        return LoginResponse.of(member, accessToken, refreshToken);
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
