package umc.catchy.domain.group.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.catchy.domain.group.dao.GroupRepository;
import umc.catchy.domain.group.domain.Groups;
import umc.catchy.domain.group.dto.request.InviteCodeRequest;
import umc.catchy.domain.group.dto.response.GroupInfoResponse;
import umc.catchy.domain.group.dto.response.GroupJoinResponse;
import umc.catchy.domain.mapping.memberGroup.dao.MemberGroupRepository;
import umc.catchy.domain.mapping.memberGroup.domain.MemberGroup;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.global.common.response.status.ErrorStatus;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final MemberGroupRepository memberGroupRepository;

    @Transactional
    public GroupJoinResponse joinGroupByInviteCode(InviteCodeRequest request) {
        Groups group = groupRepository.findByInviteCode(request.getInviteCode())
                .orElseThrow(() -> new IllegalArgumentException(ErrorStatus.GROUP_INVITE_CODE_INVALID.getMessage()));

        if (memberGroupRepository.existsByGroupIdAndMemberId(group.getId(), request.getMemberId())) {
            throw new IllegalArgumentException(ErrorStatus.GROUP_MEMBER_ALREADY_EXISTS.getMessage());
        }

        MemberGroup memberGroup = MemberGroup.builder()
                .promiseTime(group.getPromiseTime())
                .group(group)
                .member(new Member(request.getMemberId()))
                .build();
        memberGroupRepository.save(memberGroup);

        return new GroupJoinResponse(true, "Successfully joined the group.");
    }

    @Transactional(readOnly = true)
    public GroupInfoResponse getGroupInfoByInviteCode(String inviteCode) {
        Groups group = groupRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new IllegalArgumentException(ErrorStatus.GROUP_INVITE_CODE_INVALID.getMessage()));

        return GroupInfoResponse.builder()
                .groupName(group.getGroupName())
                .groupLocation(group.getGroupLocation())
                .promiseTime(group.getPromiseTime())
                .groupImage(group.getGroupImage())
                .build();
    }
}
