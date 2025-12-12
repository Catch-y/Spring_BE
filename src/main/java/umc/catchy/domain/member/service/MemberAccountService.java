package umc.catchy.domain.member.service;

import lombok.RequiredArgsConstructor;
import net.minidev.json.parser.ParseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
@Transactional(readOnly = true)
public class MemberAccountService {

    private static final String PROFILE_IMAGE_PREFIX = "profile-images/";

    private final MemberRepository memberRepository;
    private final OAuthService OAuthService;
    private final JwtTokenService tokenService;
    private final AmazonS3Manager s3Manager;

    @Transactional
    public SignUpResponse signUp(SignUpRequest request, MultipartFile profileImage, SocialType socialType) {
        SocialUserInfo socialUserInfo = OAuthService.getUserInfo(request.accessToken(), socialType);
        validateDuplicates(socialUserInfo, request.nickname());

        String profileImageUrl = uploadProfileImage(profileImage);

        Member newMember = createMember(request, socialUserInfo, profileImageUrl, socialType);
        memberRepository.save(newMember);

        handleAppleSpecifics(newMember, request, socialType);

        TokenPair tokens = tokenService.issueTokens(newMember.getEmail(), newMember.getId());

        return SignUpResponse.of(newMember, tokens);
    }

    public LoginResponse login(LoginRequest request, SocialType socialType) {
        SocialUserInfo socialUserInfo = OAuthService.getUserInfo(request.accessToken(), socialType);

        Member member = findMemberBySocialInfo(socialUserInfo);

        TokenPair tokens = tokenService.issueTokens(member.getEmail(), member.getId());

        return LoginResponse.of(member, tokens);
    }

    public ReIssueTokenResponse reIssueRefreshToken() {
        String accessToken = SecurityUtil.getCurrentAccessToken();
        String refreshToken = SecurityUtil.extractRefreshToken();

        if (refreshToken == null) {
            throw new GeneralException(ErrorStatus.NOT_FOUND_TOKEN);
        }

        TokenPair newTokens = tokenService.reissueTokens(accessToken, refreshToken);

        return ReIssueTokenResponse.of(newTokens);
    }

    @Transactional
    public void logout() {
        String accessToken = SecurityUtil.getCurrentAccessToken();
        String refreshToken = SecurityUtil.extractRefreshToken();

        tokenService.invalidateTokens(accessToken, refreshToken);
    }

    @Transactional
    public void withdraw(String authorizationCode) throws IOException, ParseException {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = findMemberById(memberId);

        if (member.getSocialType() == SocialType.APPLE) {
            if (authorizationCode == null) {
                throw new GeneralException(ErrorStatus.AUTHORIZATION_CODE_NOT_FOUND);
            }
            OAuthService.appleWithdraw(authorizationCode);
        }

        String accessToken = SecurityUtil.getCurrentAccessToken();
        String refreshToken = SecurityUtil.extractRefreshToken();
        tokenService.invalidateTokens(accessToken, refreshToken);

        memberRepository.deleteAllRelatedEntities(memberId);
        memberRepository.delete(member);
    }

    @Transactional
    public void toggleAppAlarm() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member currentMember = findMemberById(memberId);
        currentMember.toggleAppAlarmState(currentMember.getFcmInfo());
    }

    @Transactional
    public void updateFcmToken(UpdateFcmTokenRequest request) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member currentMember = findMemberById(memberId);
        currentMember.updateFcmToken(currentMember.getFcmInfo(), request.fcmToken());
    }

    private String uploadProfileImage(MultipartFile profileImage) {
        if (profileImage == null) return null;
        String keyName = PROFILE_IMAGE_PREFIX + UUID.randomUUID();
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
            member.updateAuthorizationCode(authorizationCode);
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

    private Member findMemberBySocialInfo(SocialUserInfo socialUserInfo) {
        return memberRepository.findByEmailAndProviderId(
                socialUserInfo.email(), socialUserInfo.providerId()
        ).orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
    }

    private Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
    }
}
