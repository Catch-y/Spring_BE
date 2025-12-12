package umc.catchy.domain.member.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.minidev.json.parser.ParseException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.FcmInfo;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.member.domain.SocialType;
import umc.catchy.domain.member.dto.request.LoginRequest;
import umc.catchy.domain.member.dto.request.SignUpRequest;
import umc.catchy.domain.member.dto.request.UpdateFcmTokenRequest;
import umc.catchy.domain.member.dto.response.LoginResponse;
import umc.catchy.domain.member.dto.response.ReIssueTokenResponse;
import umc.catchy.domain.member.dto.response.SignUpResponse;
import umc.catchy.domain.member.dto.response.TokenPair;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;
import umc.catchy.infra.aws.s3.AmazonS3Manager;
import umc.catchy.domain.member.service.OAuthService.SocialUserInfo;


import java.io.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberAccountService {
    private final MemberRepository memberRepository;
    private final OAuthService OAuthService;
    private final JwtTokenService tokenService;
    private final AmazonS3Manager s3Manager;

    /* 회원 가입 */
    public SignUpResponse signUp(SignUpRequest request, MultipartFile profileImage, SocialType socialType) {
        // 1. 소셜 정보 획득 및 검증
        SocialUserInfo socialUserInfo = OAuthService.getUserInfo(request.accessToken(), socialType);
        validateDuplicates(socialUserInfo, request.nickname());

        // 2. 프로필 이미지 업로드
        String profileImageUrl = uploadProfileImage(profileImage);

        // 3. 회원 생성
        Member newMember = createMember(request, socialUserInfo, profileImageUrl, socialType);
        memberRepository.save(newMember);

        // 4. 애플 회원가입이면 인가 코드를 저장
        handleAppleSpecifics(newMember, request, socialType);

        // 5. 토큰 발급
        TokenPair tokens = tokenService.issueTokens(newMember.getEmail(), newMember.getId());

        return SignUpResponse.of(newMember, tokens);
    }

    /* 로그인 */
    public LoginResponse login(LoginRequest request, SocialType socialType) {
        // 1. 소셜 정보 불러오기
        SocialUserInfo socialUserInfo = OAuthService.getUserInfo(request.accessToken(), socialType);

        // 2. 회원 조회
        Member member = findMemberBySocialInfo(socialUserInfo);

        // 3. 토큰 발급
        TokenPair tokens = tokenService.issueTokens(member.getEmail(), member.getId());

        return LoginResponse.of(member, tokens);
    }

    /* 토큰 재발급 */
    public ReIssueTokenResponse reIssueRefreshToken() {
        String accessToken = SecurityUtil.getCurrentAccessToken();
        String refreshToken = SecurityUtil.extractRefreshToken();

        if (refreshToken == null) {
            throw new GeneralException(ErrorStatus.NOT_FOUND_TOKEN);
        }

        TokenPair newTokens = tokenService.reissueTokens(accessToken, refreshToken);

        return ReIssueTokenResponse.of(newTokens);
    }

    /* 로그아웃 */
    public void logout() {
        // 1. 현재 토큰들 추출
        String accessToken = SecurityUtil.getCurrentAccessToken();
        String refreshToken = SecurityUtil.extractRefreshToken();

        // 2. 토큰 무효화
        tokenService.invalidateTokens(accessToken, refreshToken);
    }

    /* 회원 탈퇴 */
    public void withdraw(String authorizationCode) throws IOException, ParseException {
        // 1. 사용자 정보 조회
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = findMemberById(memberId);

        // 2. 애플이면 연동 해제
        if (member.getSocialType() == SocialType.APPLE) {
            if (authorizationCode == null) {
                throw new GeneralException(ErrorStatus.AUTHORIZATION_CODE_NOT_FOUND);
            }
            OAuthService.appleWithdraw(authorizationCode);
        }

        // 3. 토큰 무효화
        String accessToken = SecurityUtil.getCurrentAccessToken();
        String refreshToken = SecurityUtil.extractRefreshToken();
        tokenService.invalidateTokens(accessToken, refreshToken);

        // 4. 관련 엔티티 삭제
        memberRepository.deleteAllRelatedEntities(memberId);
        memberRepository.delete(member);
    }

    public void toggleAppAlarm() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member currentMember = memberRepository.findById(memberId).orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
        currentMember.toggleAppAlarmState(currentMember.getFcmInfo());
    }

    public void updateFcmToken(UpdateFcmTokenRequest request) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member currentMember = memberRepository.findById(memberId).orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
        currentMember.updateFcmToken(currentMember.getFcmInfo(),request.fcmToken());
    }

    private String uploadProfileImage(MultipartFile profileImage) {
        if (profileImage == null) return null;
        String keyName = "profile-images/" + UUID.randomUUID();
        return s3Manager.uploadFile(keyName, profileImage);
    }

    private void validateDuplicates(SocialUserInfo socialUserInfo, String nickname) {
        memberRepository.findByProviderId(socialUserInfo.providerId())
                .ifPresent(member -> { throw new GeneralException(ErrorStatus.PROVIDER_ID_DUPLICATE); });

        memberRepository.findByEmail(socialUserInfo.email())
                .ifPresent(member -> { throw new GeneralException(ErrorStatus.EMAIL_DUPLICATE); });

        memberRepository.findByNickname(nickname)
                .ifPresent(member -> { throw new GeneralException(ErrorStatus.NICKNAME_DUPLICATE); });
    }

    private void handleAppleSpecifics(Member member, SignUpRequest request, SocialType socialType) {
        if (socialType == SocialType.APPLE) {
            String authorizationCode = request.authorizationCode();
            if (authorizationCode == null) {
                throw new GeneralException(ErrorStatus.AUTHORIZATION_CODE_NOT_FOUND);
            }
            member.setAuthorizationCode(authorizationCode);
        }
    }

    private Member createMember(SignUpRequest request, SocialUserInfo socialUserInfo,
                                      String profileImageUrl, SocialType socialType) {
        return Member.createMember(
                socialUserInfo.providerId(),
                socialUserInfo.email(),
                request.nickname(),
                profileImageUrl,
                socialType,
                FcmInfo.createFcmInfo());
    }

    /* 소셜 정보로 멤버 조회 */
    private Member findMemberBySocialInfo(SocialUserInfo socialUserInfo) {
        return memberRepository.findByEmailAndProviderId(
                socialUserInfo.email(), socialUserInfo.providerId()
        ).orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
    }

    /* memberId로 멤버 조회 */
    private Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
    }
}
