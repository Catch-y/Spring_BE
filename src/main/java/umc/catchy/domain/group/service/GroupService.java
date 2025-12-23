package umc.catchy.domain.group.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import umc.catchy.domain.group.dao.GroupRepository;
import umc.catchy.domain.group.domain.Groups;
import umc.catchy.domain.group.dto.request.CreateGroupRequest;
import umc.catchy.domain.group.dto.response.*;
import umc.catchy.domain.mapping.memberGroup.dao.MemberGroupRepository;
import umc.catchy.domain.mapping.memberGroup.domain.MemberGroup;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;
import umc.catchy.infra.aws.s3.AmazonS3Manager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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

        MemberGroup memberGroup = MemberGroup.create(group, member);
        memberGroupRepository.save(memberGroup);

        return GroupJoinResponse.of(true, "Successfully joined the group.");
    }

    @Transactional(readOnly = true)
    public GroupInfoResponse getGroupInfoByInviteCode(String inviteCode) {
        Groups group = groupRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_INVITE_CODE_INVALID));

        return GroupInfoResponse.from(group);
    }

    @Transactional
    public CreateGroupResponse createGroup(CreateGroupRequest request, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        String groupImageUrl = null;
        MultipartFile groupImageFile = request.groupImage();
        if (groupImageFile != null && !groupImageFile.isEmpty()) {
            String keyName = "group-images/" + groupImageFile.getOriginalFilename();
            groupImageUrl = amazonS3Manager.uploadFile(keyName, groupImageFile);
        }

        // promiseTime 처리
        LocalDateTime promiseTime = request.promiseTime()
                .withSecond(0)
                .withNano(0);


        Groups group = Groups.create(request, groupImageUrl, promiseTime);
        Groups savedGroup = groupRepository.save(group);

        MemberGroup memberGroup = MemberGroup.create(savedGroup, member);
        memberGroupRepository.save(memberGroup);

        return CreateGroupResponse.of(savedGroup, member.getNickname());
    }

    @Transactional
    public void leaveGroup(Long groupId) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        Groups group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_NOT_FOUND));

        MemberGroup memberGroup = memberGroupRepository.findByGroupIdAndMemberId(group.getId(), memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_MEMBER_NOT_FOUND));

        memberGroupRepository.delete(memberGroup);
    }

    @Transactional(readOnly = true)
    public List<GroupCalendarResponse> getUserGroups(int year, int month) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime end = start.plusMonths(1);

        List<MemberGroup> memberGroups = memberGroupRepository.findAllByMemberIdAndDateRange(memberId, start, end);

        return memberGroups.stream()
                .map(MemberGroup::getGroup)
                .map(GroupCalendarResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GroupMemberResponse> getGroupMembers(Long groupId) {
        List<Member> members = memberGroupRepository.findMembersByGroupId(groupId);

        return members.stream()
                .map(GroupMemberResponse::fromEntity)
                .toList();
    }
}
