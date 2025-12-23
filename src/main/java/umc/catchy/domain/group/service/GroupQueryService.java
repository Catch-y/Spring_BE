package umc.catchy.domain.group.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.catchy.domain.group.dao.GroupRepository;
import umc.catchy.domain.group.domain.Groups;
import umc.catchy.domain.group.dto.response.GroupCalendarResponse;
import umc.catchy.domain.group.dto.response.GroupInfoResponse;
import umc.catchy.domain.group.dto.response.GroupMemberResponse;
import umc.catchy.domain.mapping.memberGroup.dao.MemberGroupRepository;
import umc.catchy.domain.mapping.memberGroup.domain.MemberGroup;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupQueryService {

    private final GroupRepository groupRepository;
    private final MemberGroupRepository memberGroupRepository;
    private final MemberRepository memberRepository;

    public List<GroupCalendarResponse> getUserGroups(Long memberId, int year, int month) {
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

    public GroupInfoResponse getGroupInfoByInviteCode(String inviteCode) {
        Groups group = groupRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_INVITE_CODE_INVALID));

        return GroupInfoResponse.from(group);
    }

    public List<GroupMemberResponse> getGroupMembers(Long groupId) {
        List<Member> members = memberGroupRepository.findMembersByGroupId(groupId);

        return members.stream()
                .map(GroupMemberResponse::fromEntity)
                .toList();
    }
}
