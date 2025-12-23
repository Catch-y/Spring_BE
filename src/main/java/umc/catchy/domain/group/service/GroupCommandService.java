package umc.catchy.domain.group.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.catchy.domain.group.dao.GroupRepository;
import umc.catchy.domain.group.domain.Groups;
import umc.catchy.domain.group.dto.request.CreateGroupRequest;
import umc.catchy.domain.group.dto.response.CreateGroupResponse;
import umc.catchy.domain.mapping.memberGroup.dao.MemberGroupRepository;
import umc.catchy.domain.mapping.memberGroup.domain.MemberGroup;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupCommandService {

    private final GroupRepository groupRepository;
    private final MemberGroupRepository memberGroupRepository;
    private final MemberRepository memberRepository;

    public CreateGroupResponse createGroupMetadata(CreateGroupRequest request, Long memberId, String imageUrl, LocalDateTime promiseTime) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        Groups group = Groups.create(request, imageUrl, promiseTime);
        Groups savedGroup = groupRepository.save(group);

        MemberGroup memberGroup = MemberGroup.create(savedGroup, member);
        memberGroupRepository.save(memberGroup);

        return CreateGroupResponse.of(savedGroup, member.getNickname());
    }

    public void joinGroup(Long memberId, String inviteCode) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        Groups group = groupRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_INVITE_CODE_INVALID));

        if (memberGroupRepository.existsByGroupIdAndMemberId(group.getId(), memberId)) {
            throw new GeneralException(ErrorStatus.GROUP_MEMBER_ALREADY_EXISTS);
        }

        MemberGroup memberGroup = MemberGroup.create(group, member);
        memberGroupRepository.save(memberGroup);
    }

    public void deleteMemberGroup(Long groupId, Long memberId) {
        MemberGroup memberGroup = memberGroupRepository.findByGroupIdAndMemberId(groupId, memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_MEMBER_NOT_FOUND));

        memberGroupRepository.delete(memberGroup);
    }
}
