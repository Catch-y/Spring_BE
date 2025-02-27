package umc.catchy.domain.group.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import umc.catchy.domain.group.dao.GroupRepository;
import umc.catchy.domain.group.domain.Groups;
import umc.catchy.domain.group.dto.request.CreateGroupRequest;
import umc.catchy.domain.group.dto.response.CreateGroupResponse;
import umc.catchy.domain.group.dto.response.GroupCalendarResponse;
import umc.catchy.domain.group.dto.response.GroupInfoResponse;
import umc.catchy.domain.group.dto.response.GroupJoinResponse;
import umc.catchy.domain.group.dto.response.GroupMemberResponse;
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

        // promiseTime 처리
        LocalDateTime promiseTime = request.getPromiseTime()
                .withSecond(0)
                .withNano(0);


        Groups group = Groups.builder()
                .groupName(request.getGroupName())
                .groupImage(groupImageUrl)
                .groupLocation(request.getGroupLocation())
                .inviteCode(request.getInviteCode())
                .promiseTime(promiseTime)
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

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        List<MemberGroup> memberGroups = memberGroupRepository.findAllByMemberId(memberId);

        // 해당 년도와 월에 맞는 그룹 필터링
        return memberGroups.stream()
                .filter(memberGroup -> {
                    LocalDateTime promiseTime = memberGroup.getGroup().getPromiseTime();
                    return promiseTime.getYear() == year && promiseTime.getMonthValue() == month;
                })
                .map(memberGroup -> {
                    Groups group = memberGroup.getGroup();
                    return GroupCalendarResponse.builder()
                            .groupId(group.getId())
                            .groupName(group.getGroupName())
                            .promiseTime(group.getPromiseTime())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GroupMemberResponse> getGroupMembers(Long groupId) {
        List<Member> members = memberGroupRepository.findMembersByGroupId(groupId);

        return members.stream()
                .map(GroupMemberResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
