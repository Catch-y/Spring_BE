package umc.catchy.domain.group.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import umc.catchy.domain.group.dto.request.CreateGroupRequest;
import umc.catchy.domain.group.dto.request.InviteCodeRequest;
import umc.catchy.domain.group.dto.response.*;
import umc.catchy.domain.group.service.GroupFacade;
import umc.catchy.support.ControllerTestSupport;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GroupController.class)
class GroupControllerTest extends ControllerTestSupport {

    @MockitoBean
    private GroupFacade groupFacade;

    @Test
    @DisplayName("그룹 생성 API 테스트")
    void createGroup_api_test() throws Exception {
        CreateGroupResponse response = new CreateGroupResponse(
                1L, "테스트 그룹", "서울시 강남구", "https://image.url", "ABC1234",
                LocalDateTime.of(2025, 1, 25, 15, 30), "테스트유저"
        );

        when(groupFacade.createGroup(any(CreateGroupRequest.class))).thenReturn(response);

        mockMvc.perform(multipart("/group")
                        .file("groupImage", "test.jpg".getBytes())
                        .param("groupName", "테스트 그룹")
                        .param("groupLocation", "서울시 강남구")
                        .param("promiseTime", "2025-01-25T15:30:00")
                        .param("inviteCode", "ABC1234")
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.groupId").value(1L));
    }

    @Test
    @DisplayName("초대 코드로 그룹 가입 API 테스트")
    void joinGroupByInviteCode_api_test() throws Exception {
        InviteCodeRequest request = new InviteCodeRequest("ABC1234");
        GroupJoinResponse response = GroupJoinResponse.of(true, "Successfully joined");

        when(groupFacade.joinGroupByInviteCode(anyString())).thenReturn(response);

        mockMvc.perform(post("/group/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));
    }

    @Test
    @DisplayName("사용자가 속한 그룹 조회 API 테스트")
    void getUserGroups_api_test() throws Exception {
        GroupCalendarResponse response = new GroupCalendarResponse(1L, "1월 약속", LocalDateTime.now());
        when(groupFacade.getUserGroups(anyInt(), anyInt())).thenReturn(List.of(response));

        mockMvc.perform(get("/group/my-groups")
                        .param("year", "2025")
                        .param("month", "1")
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));
    }
}
