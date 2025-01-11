package umc.catchy.domain.group.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import umc.catchy.domain.group.dto.request.InviteCodeRequest;
import umc.catchy.domain.group.dto.response.GroupJoinResponse;
import umc.catchy.domain.group.service.GroupService;
import umc.catchy.global.common.response.BaseResponse;
import umc.catchy.global.common.response.status.SuccessStatus;

@Tag(name = "Group", description = "그룹 관련 API")
@RestController
@RequestMapping("/group")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @Operation(summary = "그룹 초대 코드로 가입", description = "초대 코드를 입력하여 사용자가 그룹에 가입합니다.")
    @PostMapping("/join")
    public ResponseEntity<BaseResponse<GroupJoinResponse>> joinGroupByInviteCode(@Valid @RequestBody InviteCodeRequest request) {
        GroupJoinResponse response = groupService.joinGroupByInviteCode(request);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, response));
    }
}