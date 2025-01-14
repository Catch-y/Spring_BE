package umc.catchy.domain.group.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import umc.catchy.domain.group.dto.request.CreateGroupRequest;
import umc.catchy.domain.group.dto.request.InviteCodeRequest;
import umc.catchy.domain.group.dto.response.CreateGroupResponse;
import umc.catchy.domain.group.dto.response.GroupInfoResponse;
import umc.catchy.domain.group.dto.response.GroupJoinResponse;
import umc.catchy.domain.group.service.GroupService;
import umc.catchy.global.common.response.BaseResponse;
import umc.catchy.global.common.response.status.SuccessStatus;
import umc.catchy.global.util.SecurityUtil;

@Tag(name = "Group", description = "그룹 관련 API")
@RestController
@RequestMapping("/group")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @Operation(summary = "그룹 생성", description = "그룹을 생성합니다.")
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<BaseResponse<CreateGroupResponse>> createGroup(@Validated @ModelAttribute CreateGroupRequest request) {

        Long memberId = SecurityUtil.getCurrentMemberId();
        CreateGroupResponse response = groupService.createGroup(request, memberId);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._CREATED, response));
    }

    @Operation(summary = "그룹 초대 코드로 가입", description = "초대 코드를 입력하여 사용자가 그룹에 가입합니다.")
    @PostMapping("/join")
    public ResponseEntity<BaseResponse<GroupJoinResponse>> joinGroupByInviteCode(@Valid @RequestBody InviteCodeRequest request) {
        GroupJoinResponse response = groupService.joinGroupByInviteCode(request.getInviteCode());
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, response));
    }

    @Operation(summary = "초대 코드로 그룹 정보 조회", description = "초대 코드를 이용하여 그룹 이름, 장소, 약속 날짜 및 그룹 이미지를 조회합니다.")
    @GetMapping("/invite/{inviteCode}")
    public ResponseEntity<BaseResponse<GroupInfoResponse>> getGroupInfoByInviteCode(
            @Parameter(description = "그룹 초대 코드", required = true)
            @PathVariable String inviteCode
    ) {
        GroupInfoResponse response = groupService.getGroupInfoByInviteCode(inviteCode);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, response));
    }

    @Operation(summary = "그룹 탈퇴", description = "로그인한 사용자가 특정 그룹에서 탈퇴합니다.")
    @DeleteMapping("/{groupId}/leave")
    public ResponseEntity<BaseResponse<Void>> leaveGroup(
            @Parameter(description = "그룹 ID", required = true)
            @PathVariable Long groupId
    ) {
        groupService.leaveGroup(groupId);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, null));
    }
}