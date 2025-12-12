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
@Transactional(readOnly = true)
@Slf4j
public class MemberProfileService {

    private static final String PROFILE_IMAGE_PREFIX = "profile-images/";

    private final MemberRepository memberRepository;
    private final AmazonS3Manager s3Manager;

    public ProfileResponse getCurrentMember() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = findMemberById(memberId);

        return ProfileResponse.of(member);
    }

    @Transactional
    public NicknameResponse updateNickname(NicknameRequest request) {
        validateNicknameDuplicate(request.nickname());

        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = findMemberById(memberId);

        member.updateNickname(request.nickname());

        return NicknameResponse.of(member);
    }

    @Transactional
    public ProfileImageResponse updateProfileImage(MultipartFile newProfileImage) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = findMemberById(memberId);

        String originProfileImageUrl = member.getProfileImage();

        if (originProfileImageUrl != null) {
            s3Manager.deleteImage(originProfileImageUrl);
        }

        String keyName = PROFILE_IMAGE_PREFIX + UUID.randomUUID();
        String newProfileImageUrl = s3Manager.uploadFile(keyName, newProfileImage);

        member.updateProfileImage(newProfileImageUrl);

        return ProfileImageResponse.of(member);
    }

    public void validateNickname(NicknameRequest request) {
        validateNicknameDuplicate(request.nickname());
    }

    private void validateNicknameDuplicate(String nickname) {
        memberRepository.findByNickname(nickname)
                .ifPresent(member -> {
                    throw new GeneralException(ErrorStatus.NICKNAME_DUPLICATE);
                });
    }

    private Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
    }
}
