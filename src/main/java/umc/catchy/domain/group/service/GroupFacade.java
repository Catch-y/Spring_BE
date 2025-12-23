package umc.catchy.domain.group.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import umc.catchy.domain.group.domain.Groups;
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

    private static final String GROUP_IMAGE_PREFIX = "group-images/";
    private static final String JOIN_SUCCESS_MESSAGE = "Successfully joined the group.";
    private static final int TIME_NORMALIZATION_VALUE = 0;

    private final GroupCommandService groupCommandService;
    private final GroupQueryService groupQueryService;
    private final AmazonS3Manager amazonS3Manager;

    public CreateGroupResponse createGroup(CreateGroupRequest request) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        String groupImageUrl = null;
        MultipartFile groupImageFile = request.groupImage();

        if (groupImageFile != null && !groupImageFile.isEmpty()) {
            String keyName = GROUP_IMAGE_PREFIX + groupImageFile.getOriginalFilename();
            groupImageUrl = amazonS3Manager.uploadFile(keyName, groupImageFile);
        }

        LocalDateTime promiseTime = request.promiseTime()
                .withSecond(TIME_NORMALIZATION_VALUE)
                .withNano(TIME_NORMALIZATION_VALUE);

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
        return GroupJoinResponse.of(true, JOIN_SUCCESS_MESSAGE);
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
