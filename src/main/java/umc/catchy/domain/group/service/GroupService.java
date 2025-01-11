package umc.catchy.domain.group.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.catchy.domain.group.dao.GroupRepository;
import umc.catchy.domain.group.domain.Groups;
import umc.catchy.domain.group.dto.request.InviteCodeRequest;
import umc.catchy.domain.group.dto.response.GroupJoinResponse;
import umc.catchy.domain.mapping.memberGroup.dao.MemberGroupRepository;
import umc.catchy.domain.mapping.memberGroup.domain.MemberGroup;
import umc.catchy.domain.member.domain.Member;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final MemberGroupRepository memberGroupRepository;

    @Transactional
    public GroupJoinResponse joinGroupByInviteCode(InviteCodeRequest request) {
        Groups group = groupRepository.findByInviteCode(request.getInviteCode())
                .orElseThrow(() -> new IllegalArgumentException("Invalid invite code."));

        if (memberGroupRepository.existsByGroupIdAndMemberId(group.getId(), request.getMemberId())) {
            return new GroupJoinResponse(false, "You are already a member of this group.");
        }

        MemberGroup memberGroup = MemberGroup.builder()
                .promiseTime(group.getPromiseTime())
                .group(group)
                .member(new Member(request.getMemberId())) // MemberID 클라이언트에서 입력 -> 추후 수정
                .build();
        memberGroupRepository.save(memberGroup);

        return new GroupJoinResponse(true, "Successfully joined the group.");
    }
}
