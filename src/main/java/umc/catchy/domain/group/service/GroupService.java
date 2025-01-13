package umc.catchy.domain.group.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import umc.catchy.domain.group.dao.GroupRepository;
import umc.catchy.domain.group.domain.Groups;
import umc.catchy.domain.group.dto.request.CreateGroupRequest;
import umc.catchy.domain.group.dto.response.CreateGroupResponse;
import umc.catchy.domain.group.dto.response.GroupInfoResponse;
import umc.catchy.domain.group.dto.response.GroupJoinResponse;
import umc.catchy.domain.mapping.memberGroup.dao.MemberGroupRepository;
import umc.catchy.domain.mapping.memberGroup.domain.MemberGroup;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;
import umc.catchy.infra.aws.s3.AmazonS3Manager;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final MemberGroupRepository memberGroupRepository;
    private final MemberRepository memberRepository;
    private final AmazonS3Manager amazonS3Manager;

    @Transactional
    public GroupJoinResponse joinGroupByInviteCode(String inviteCode) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        Groups group = groupRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_INVITE_CODE_INVALID));

        if (memberGroupRepository.existsByGroupIdAndMemberId(group.getId(), memberId)) {
            throw new GeneralException(ErrorStatus.GROUP_MEMBER_ALREADY_EXISTS);
        }

        MemberGroup memberGroup = MemberGroup.builder()
                .promiseTime(group.getPromiseTime())
                .group(group)
                .member(member)
                .build();
        memberGroupRepository.save(memberGroup);

        return new GroupJoinResponse(true, "Successfully joined the group.");
    }

    @Transactional(readOnly = true)
    public GroupInfoResponse getGroupInfoByInviteCode(String inviteCode) {
        Groups group = groupRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_INVITE_CODE_INVALID));

        return GroupInfoResponse.builder()
                .groupName(group.getGroupName())
                .groupLocation(group.getGroupLocation())
                .promiseTime(group.getPromiseTime())
                .groupImage(group.getGroupImage())
                .build();
    }

    @Transactional
    public CreateGroupResponse createGroup(CreateGroupRequest request, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        String groupImageUrl = null;
        MultipartFile groupImageFile = request.getGroupImage();
        if (groupImageFile != null && !groupImageFile.isEmpty()) {
            String keyName = "group-images/" + groupImageFile.getOriginalFilename();
            groupImageUrl = amazonS3Manager.uploadFile(keyName, groupImageFile);
        }

        Groups group = Groups.builder()
                .groupName(request.getGroupName())
                .groupImage(groupImageUrl)
                .groupLocation(request.getGroupLocation())
                .inviteCode(request.getInviteCode())
                .promiseTime(request.getPromiseTime())
                .build();

        Groups savedGroup = groupRepository.save(group);

        MemberGroup memberGroup = MemberGroup.builder()
                .promiseTime(savedGroup.getPromiseTime())
                .member(member)
                .group(savedGroup)
                .build();
        memberGroupRepository.save(memberGroup);

        return CreateGroupResponse.fromEntity(savedGroup, member.getNickname());
    }
}
