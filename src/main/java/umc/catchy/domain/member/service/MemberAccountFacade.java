package umc.catchy.domain.member.service;

import lombok.RequiredArgsConstructor;
import net.minidev.json.parser.ParseException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
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

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberAccountFacade {

    private static final String PROFILE_IMAGE_PREFIX = "profile-images/";

    private final OAuthService oAuthService;
    private final JwtTokenService tokenService;
    private final AmazonS3Manager s3Manager;
    private final MemberAccountCommandService memberAccountCommandService;

    public SignUpResponse signUp(SignUpRequest request, MultipartFile profileImage, SocialType socialType) {
        SocialUserInfo socialUserInfo = oAuthService.getUserInfo(request.accessToken(), socialType);

        memberAccountCommandService.validateDuplicates(socialUserInfo, request.nickname());

        String profileImageUrl = uploadProfileImage(profileImage);

        try {
            Member newMember = createMember(request, socialUserInfo, profileImageUrl, socialType);
            handleAppleSpecifics(newMember, request, socialType);

            Member savedMember = memberAccountCommandService.saveMember(newMember);

            TokenPair tokens = tokenService.issueTokens(savedMember.getEmail(), savedMember.getId());

            return SignUpResponse.of(savedMember, tokens);

        } catch (Exception e) {
            if (profileImageUrl != null) {
                s3Manager.deleteImage(profileImageUrl);
            }
            throw e;
        }
    }

    public LoginResponse login(LoginRequest request, SocialType socialType) {
        SocialUserInfo socialUserInfo = oAuthService.getUserInfo(request.accessToken(), socialType);

        Member member = memberAccountCommandService.findMemberBySocialInfo(socialUserInfo);

        TokenPair tokens = tokenService.issueTokens(member.getEmail(), member.getId());

        return LoginResponse.of(member, tokens);
    }

    public void withdraw(String authorizationCode) throws IOException, ParseException {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberAccountCommandService.findMemberById(memberId);

        if (member.getSocialType() == SocialType.APPLE) {
            if (authorizationCode == null) {
                throw new GeneralException(ErrorStatus.AUTHORIZATION_CODE_NOT_FOUND);
            }
            oAuthService.appleWithdraw(authorizationCode);
        }

        String accessToken = SecurityUtil.getCurrentAccessToken();
        String refreshToken = SecurityUtil.extractRefreshToken();
        tokenService.invalidateTokens(accessToken, refreshToken);

        memberAccountCommandService.deleteMember(memberId);
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

    public void logout() {
        String accessToken = SecurityUtil.getCurrentAccessToken();
        String refreshToken = SecurityUtil.extractRefreshToken();
        tokenService.invalidateTokens(accessToken, refreshToken);
    }

    public void toggleAppAlarm() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        memberAccountCommandService.toggleAppAlarm(memberId);
    }

    public void updateFcmToken(UpdateFcmTokenRequest request) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        memberAccountCommandService.updateFcmToken(memberId, request.fcmToken());
    }

    private String uploadProfileImage(MultipartFile profileImage) {
        if (profileImage == null) return null;
        String keyName = PROFILE_IMAGE_PREFIX + UUID.randomUUID();
        return s3Manager.uploadFile(keyName, profileImage);
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
}
