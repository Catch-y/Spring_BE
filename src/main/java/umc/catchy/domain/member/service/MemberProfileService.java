package umc.catchy.domain.member.service;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.member.dto.request.NicknameRequest;
import umc.catchy.domain.member.dto.response.*;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;
import umc.catchy.infra.aws.s3.AmazonS3Manager;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MemberProfileService {
    private final MemberRepository memberRepository;
    private final AmazonS3Manager s3Manager;

    /* 회원 프로필 조회 */
    public ProfileResponse getCurrentMember() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId).orElseThrow(() ->
                new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        return ProfileResponse.of(member);
    }

    /* 닉네임 변경 */
    public NicknameResponse updateNickname(NicknameRequest request) {
        // 닉네임 중복 검사
        memberRepository.findByNickname(request.nickname())
                .ifPresent(member -> {
                    throw new GeneralException(ErrorStatus.NICKNAME_DUPLICATE);
                });

        Long memberId = SecurityUtil.getCurrentMemberId();

        Member member = memberRepository.findById(memberId).orElseThrow(() ->
                new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        // 닉네임 변경
        member.setNickname(request.nickname());

        return NicknameResponse.of(member);
    }

    /* 프로필 사진 변경 */
    public ProfileImageResponse updateProfileImage(MultipartFile newProfileImage) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId).orElseThrow(() ->
                new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        String originProfileImageUrl = member.getProfileImage();

        // 기존에 프로필 사진이 있었다면 제거
        if (originProfileImageUrl != null) {
            s3Manager.deleteImage(originProfileImageUrl);
        }

        // 프로필 사진 url 생성
        String keyName = "profile-images/" + UUID.randomUUID();
        String newProfileImageUrl = s3Manager.uploadFile(keyName, newProfileImage);

        // 이미지 변경
        member.setProfileImage(newProfileImageUrl);

        return ProfileImageResponse.of(member);
    }

    /* 닉네임 중복 검사 */
    public void validateNickname(NicknameRequest request) {
        String nickname = request.nickname();

        // 닉네임 중복 검사
        memberRepository.findByNickname(nickname)
                .ifPresent(member -> {
                    throw new GeneralException(ErrorStatus.NICKNAME_DUPLICATE);
                });
    }
}

