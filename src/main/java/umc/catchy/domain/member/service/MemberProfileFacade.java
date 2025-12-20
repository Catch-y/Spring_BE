package umc.catchy.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.member.dto.request.NicknameRequest;
import umc.catchy.domain.member.dto.response.NicknameResponse;
import umc.catchy.domain.member.dto.response.ProfileImageResponse;
import umc.catchy.domain.member.dto.response.ProfileResponse;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;
import umc.catchy.infra.aws.s3.AmazonS3Manager;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberProfileFacade {

    private static final String PROFILE_IMAGE_PREFIX = "profile-images/";

    private final MemberRepository memberRepository;
    private final AmazonS3Manager s3Manager;
    private final MemberProfileCommandService memberProfileCommandService;

    public ProfileResponse getCurrentMember() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = findMemberById(memberId);
        return ProfileResponse.of(member);
    }

    public NicknameResponse updateNickname(NicknameRequest request) {
        validateNicknameDuplicate(request.nickname());

        Long memberId = SecurityUtil.getCurrentMemberId();
        return memberProfileCommandService.updateNickname(memberId, request.nickname());
    }

    public ProfileImageResponse updateProfileImage(MultipartFile newProfileImage) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = findMemberById(memberId);

        String oldUrl = member.getProfileImage();
        String newUrl = null;

        try {
            if (oldUrl != null) {
                s3Manager.deleteImage(oldUrl);
            }

            String keyName = PROFILE_IMAGE_PREFIX + UUID.randomUUID();
            newUrl = s3Manager.uploadFile(keyName, newProfileImage);

            return memberProfileCommandService.updateProfileImage(memberId, newUrl);

        } catch (Exception e) {
            if (newUrl != null) {
                s3Manager.deleteImage(newUrl);
            }
            throw e;
        }
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
