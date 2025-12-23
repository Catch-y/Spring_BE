package umc.catchy.domain.group.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import umc.catchy.domain.group.dto.request.CreateGroupRequest;
import umc.catchy.domain.group.dto.response.CreateGroupResponse;
import umc.catchy.domain.group.dto.response.GroupCalendarResponse;
import umc.catchy.domain.group.dto.response.GroupInfoResponse;
import umc.catchy.domain.group.dto.response.GroupJoinResponse;
import umc.catchy.domain.group.dto.response.GroupMemberResponse;
import umc.catchy.global.util.SecurityUtil;
import umc.catchy.infra.aws.s3.AmazonS3Manager;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GroupFacade {

    private final GroupCommandService groupCommandService;
    private final GroupQueryService groupQueryService;
    private final AmazonS3Manager amazonS3Manager;

    public CreateGroupResponse createGroup(CreateGroupRequest request) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        String groupImageUrl = null;
        MultipartFile groupImageFile = request.groupImage();

        if (groupImageFile != null && !groupImageFile.isEmpty()) {
            String keyName = "group-images/" + groupImageFile.getOriginalFilename();
            groupImageUrl = amazonS3Manager.uploadFile(keyName, groupImageFile);
        }

        LocalDateTime promiseTime = request.promiseTime()
                .withSecond(0)
                .withNano(0);

        try {
            return groupCommandService.createGroupMetadata(request, memberId, groupImageUrl, promiseTime);
        } catch (Exception e) {
            if (groupImageUrl != null) {
                amazonS3Manager.deleteImage(groupImageUrl);
            }
            throw e;
        }
    }

    public GroupJoinResponse joinGroupByInviteCode(String inviteCode) {
        groupCommandService.joinGroup(SecurityUtil.getCurrentMemberId(), inviteCode);
        return GroupJoinResponse.of(true, "Successfully joined the group.");
    }

    public void leaveGroup(Long groupId) {
        groupCommandService.deleteMemberGroup(groupId, SecurityUtil.getCurrentMemberId());
    }

    public List<GroupCalendarResponse> getUserGroups(int year, int month) {
        return groupQueryService.getUserGroups(SecurityUtil.getCurrentMemberId(), year, month);
    }

    public GroupInfoResponse getGroupInfoByInviteCode(String inviteCode) {
        return groupQueryService.getGroupInfoByInviteCode(inviteCode);
    }

    public List<GroupMemberResponse> getGroupMembers(Long groupId) {
        return groupQueryService.getGroupMembers(groupId);
    }
}
