package umc.catchy.domain.group.api;

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

@RestController
@RequestMapping("/group")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping("/join")
    public ResponseEntity<GroupJoinResponse> joinGroupByInviteCode(@Valid @RequestBody InviteCodeRequest request) {
        GroupJoinResponse response = groupService.joinGroupByInviteCode(request);
        return ResponseEntity.ok(response);
    }
}