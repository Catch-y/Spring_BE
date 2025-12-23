package umc.catchy.domain.group.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import umc.catchy.domain.group.dto.request.CreateGroupRequest;
import umc.catchy.domain.group.dto.request.InviteCodeRequest;
import umc.catchy.domain.group.dto.response.*;
import umc.catchy.domain.group.service.GroupService;
import umc.catchy.global.util.SecurityUtil;
import umc.catchy.support.ControllerTestSupport;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GroupController.class)
class GroupControllerTest extends ControllerTestSupport {

    @MockitoBean
    private GroupService groupService;

    @Test
    @DisplayName("그룹 생성 API 테스트 (Multipart/form-data)")
    void createGroup_api_test() throws Exception {
        // given
        CreateGroupResponse response = new CreateGroupResponse(
                1L, "테스트 그룹", "서울시 강남구", "https://image.url", "ABC1234",
                LocalDateTime.of(2025, 1, 25, 15, 30), "테스트유저"
        );

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentMemberId).thenReturn(1L);
            when(groupService.createGroup(any(CreateGroupRequest.class), eq(1L))).thenReturn(response);

            // when & then
            mockMvc.perform(multipart("/group")
                            .file("groupImage", "test.jpg".getBytes())
                            .param("groupName", "테스트 그룹")
                            .param("groupLocation", "서울시 강남구")
                            .param("promiseTime", "2025-01-25T15:30:00")
                            .param("inviteCode", "ABC1234")
                            .header("Authorization", testToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.groupId").value(1L))
                    .andExpect(jsonPath("$.result.groupName").value("테스트 그룹"));
        }
    }

    @Test
    @DisplayName("초대 코드로 그룹 가입 API 테스트")
    void joinGroupByInviteCode_api_test() throws Exception {
        // given
        InviteCodeRequest request = new InviteCodeRequest("ABC1234");
        GroupJoinResponse response = GroupJoinResponse.of(true, "Successfully joined");

        when(groupService.joinGroupByInviteCode(anyString())).thenReturn(response);

        // when & then
        mockMvc.perform(post("/group/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.success").value(true));
    }

    @Test
    @DisplayName("사용자가 속한 그룹 조회 API 테스트")
    void getUserGroups_api_test() throws Exception {
        // given
        GroupCalendarResponse response = new GroupCalendarResponse(1L, "1월 약속", LocalDateTime.now());
        when(groupService.getUserGroups(anyInt(), anyInt())).thenReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/group/my-groups")
                        .param("year", "2025")
                        .param("month", "1")
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result[0].groupName").value("1월 약속"));
    }

    @Test
    @DisplayName("그룹 탈퇴 API 테스트")
    void leaveGroup_api_test() throws Exception {
        // when & then
        mockMvc.perform(delete("/group/{groupId}/leave", 1L)
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));
    }

    @Test
    @DisplayName("그룹 멤버 조회 API 테스트")
    void getGroupMembers_api_test() throws Exception {
        // given
        GroupMemberResponse memberResponse = new GroupMemberResponse(1L, "유저1", "https://profile.jpg");
        when(groupService.getGroupMembers(anyLong())).thenReturn(List.of(memberResponse));

        // when & then
        mockMvc.perform(get("/group/{groupId}/members", 1L)
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result[0].nickname").value("유저1"));
    }
}

