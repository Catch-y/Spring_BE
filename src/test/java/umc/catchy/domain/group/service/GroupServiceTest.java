package umc.catchy.domain.group.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import umc.catchy.domain.group.dao.GroupRepository;
import umc.catchy.domain.group.domain.Groups;
import umc.catchy.domain.group.dto.request.CreateGroupRequest;
import umc.catchy.domain.group.dto.response.CreateGroupResponse;
import umc.catchy.domain.group.dto.response.GroupCalendarResponse;
import umc.catchy.domain.group.dto.response.GroupJoinResponse;
import umc.catchy.domain.group.dto.response.GroupMemberResponse;
import umc.catchy.domain.mapping.memberGroup.dao.MemberGroupRepository;
import umc.catchy.domain.mapping.memberGroup.domain.MemberGroup;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;
import umc.catchy.infra.aws.s3.AmazonS3Manager;
import umc.catchy.support.fixture.GroupFixture;
import umc.catchy.support.fixture.MemberFixture;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @InjectMocks
    private GroupService groupService;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private MemberGroupRepository memberGroupRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AmazonS3Manager amazonS3Manager;

    @Test
    @DisplayName("그룹 생성 성공 - 그룹 생성 및 생성자 멤버 등록 검증")
    void createGroup_success() {
        // given
        Long memberId = 1L;
        CreateGroupRequest request = GroupFixture.createGroupRequest("테스트 그룹", "ABC1234");
        Member testMember = MemberFixture.createTestMember(memberId, "test@test.com");

        Groups savedGroup = GroupFixture.createGroupWithId(100L, request.groupName(), request.inviteCode());

        ArgumentCaptor<MemberGroup> memberGroupCaptor = ArgumentCaptor.forClass(MemberGroup.class);

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentMemberId).thenReturn(memberId);

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(testMember));
            when(amazonS3Manager.uploadFile(anyString(), any(MultipartFile.class)))
                    .thenReturn("https://catchy-s3.com/group-images/test-image.jpg");

            when(groupRepository.save(any(Groups.class))).thenReturn(savedGroup);

            // when
            CreateGroupResponse response = groupService.createGroup(request, memberId);

            // then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.groupId()).isEqualTo(100L),
                    () -> assertThat(response.groupName()).isEqualTo("테스트 그룹"),
                    () -> {
                        verify(memberGroupRepository).save(memberGroupCaptor.capture());
                        MemberGroup capturedMemberGroup = memberGroupCaptor.getValue();
                        assertThat(capturedMemberGroup.getGroup()).isEqualTo(savedGroup);
                        assertThat(capturedMemberGroup.getMember()).isEqualTo(testMember);
                    }
            );

            verify(memberRepository, times(1)).findById(memberId);
            verify(groupRepository, times(1)).save(any(Groups.class));
            verify(memberGroupRepository, times(1)).save(any(MemberGroup.class));
        }
    }

    @Test
    @DisplayName("초대 코드로 그룹 가입 성공")
    void joinGroupByInviteCode_success() {
        // given
        Long memberId = 1L;
        String inviteCode = "ABC1234";
        Member testMember = MemberFixture.createTestMember(memberId, "test@test.com");
        Groups existingGroup = GroupFixture.createGroupWithId(100L, "기존 그룹", inviteCode);

        ArgumentCaptor<MemberGroup> memberGroupCaptor = ArgumentCaptor.forClass(MemberGroup.class);

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentMemberId).thenReturn(memberId);

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(testMember));
            when(groupRepository.findByInviteCode(inviteCode)).thenReturn(Optional.of(existingGroup));

            when(memberGroupRepository.existsByGroupIdAndMemberId(existingGroup.getId(), memberId))
                    .thenReturn(false);

            // when
            GroupJoinResponse response = groupService.joinGroupByInviteCode(inviteCode);

            // then
            assertAll(
                    () -> assertThat(response.success()).isTrue(),
                    () -> assertThat(response.message()).isEqualTo("Successfully joined the group."),
                    () -> {
                        verify(memberGroupRepository).save(memberGroupCaptor.capture());
                        MemberGroup capturedMemberGroup = memberGroupCaptor.getValue();
                        assertThat(capturedMemberGroup.getMember()).isEqualTo(testMember);
                        assertThat(capturedMemberGroup.getGroup()).isEqualTo(existingGroup);
                    }
            );

            verify(memberRepository, times(1)).findById(memberId);
            verify(groupRepository, times(1)).findByInviteCode(inviteCode);
        }
    }

    @Test
    @DisplayName("초대 코드로 그룹 가입 실패 - 이미 가입된 회원")
    void joinGroupByInviteCode_fail_already_exists() {
        // given
        Long memberId = 1L;
        String inviteCode = "ABC1234";
        Groups existingGroup = GroupFixture.createGroupWithId(100L, "기존 그룹", inviteCode);

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentMemberId).thenReturn(memberId);

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(mock(Member.class)));
            when(groupRepository.findByInviteCode(inviteCode)).thenReturn(Optional.of(existingGroup));

            when(memberGroupRepository.existsByGroupIdAndMemberId(existingGroup.getId(), memberId))
                    .thenReturn(true);

            // when & then
            assertThatThrownBy(() -> groupService.joinGroupByInviteCode(inviteCode))
                    .isInstanceOf(GeneralException.class)
                    .hasMessageContaining("이미 그룹에 가입된 회원입니다.");

            verify(memberGroupRepository, never()).save(any(MemberGroup.class));
        }
    }

    @Test
    @DisplayName("사용자가 속한 그룹 조회 - 년/월 필터링 검증")
    void getUserGroups_filtering_success() {
        // given
        Long memberId = 1L;
        int targetYear = 2025;
        int targetMonth = 1;

        Member testMember = MemberFixture.createTestMember(memberId, "test@test.com");

        // 1. 조건에 맞는 그룹 (2025년 1월)
        Groups group1 = GroupFixture.createGroupWithIdAndYearAndMonth(101L, 2025, 1);

        // 2. 조건에 맞지 않는 그룹 (2025년 2월 - 월이 다름)
        Groups group2 = GroupFixture.createGroupWithIdAndYearAndMonth(102L, 2025, 2);

        // 3. 조건에 맞지 않는 그룹 (2024년 1월 - 년도가 다름)
        Groups group3 = GroupFixture.createGroupWithIdAndYearAndMonth(103L, 2024, 1);

        MemberGroup mg1 = MemberGroup.create(group1, testMember);
        MemberGroup mg2 = MemberGroup.create(group2, testMember);
        MemberGroup mg3 = MemberGroup.create(group3, testMember);

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentMemberId).thenReturn(memberId);

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(testMember));
            when(memberGroupRepository.findAllByMemberId(memberId)).thenReturn(List.of(mg1, mg2, mg3));

            // when
            List<GroupCalendarResponse> result = groupService.getUserGroups(targetYear, targetMonth);

            // then
            assertAll(
                    () -> assertThat(result).hasSize(1),
                    () -> assertThat(result.get(0).groupId()).isEqualTo(101L),
                    () -> assertThat(result.get(0).promiseTime().getYear()).isEqualTo(targetYear),
                    () -> assertThat(result.get(0).promiseTime().getMonthValue()).isEqualTo(targetMonth)
            );

            verify(memberRepository, times(1)).findById(memberId);
            verify(memberGroupRepository, times(1)).findAllByMemberId(memberId);
        }
    }

    @Test
    @DisplayName("그룹 탈퇴 성공")
    void leaveGroup_success() {
        // given
        Long memberId = 1L;
        Long groupId = 100L;
        Member testMember = MemberFixture.createTestMember(memberId, "test@test.com");
        Groups existingGroup = GroupFixture.createGroupWithId(groupId, "탈퇴할 그룹", "BYE123");
        MemberGroup memberGroup = MemberGroup.create(existingGroup, testMember);

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentMemberId).thenReturn(memberId);

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(testMember));
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(existingGroup));
            when(memberGroupRepository.findByGroupIdAndMemberId(groupId, memberId))
                    .thenReturn(Optional.of(memberGroup));

            // when
            groupService.leaveGroup(groupId);

            // then
            verify(memberGroupRepository, times(1)).delete(memberGroup);
        }
    }

    @Test
    @DisplayName("그룹 탈퇴 실패 - 그룹에 속하지 않은 멤버")
    void leaveGroup_fail_member_not_in_group() {
        // given
        Long memberId = 1L;
        Long groupId = 100L;
        Member testMember = MemberFixture.createTestMember(memberId, "test@test.com");
        Groups existingGroup = GroupFixture.createGroupWithId(groupId, "탈퇴할 그룹", "BYE123");

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentMemberId).thenReturn(memberId);

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(testMember));
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(existingGroup));
            when(memberGroupRepository.findByGroupIdAndMemberId(groupId, memberId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> groupService.leaveGroup(groupId))
                    .isInstanceOf(GeneralException.class)
                    .hasMessageContaining("사용자가 그룹에 속해 있지 않습니다.");

            verify(memberGroupRepository, never()).delete(any(MemberGroup.class));
        }
    }

    @Test
    @DisplayName("그룹 멤버 조회 성공")
    void getGroupMembers_success() {
        // given
        Long groupId = 100L;
        Member member1 = MemberFixture.createTestMember(1L, "user1@test.com");
        Member member2 = MemberFixture.createTestMember(2L, "user2@test.com");
        List<Member> members = List.of(member1, member2);

        when(memberGroupRepository.findMembersByGroupId(groupId)).thenReturn(members);

        // when
        List<GroupMemberResponse> result = groupService.getGroupMembers(groupId);

        // then
        assertAll(
                () -> assertThat(result).hasSize(2),
                () -> assertThat(result.get(0).memberId()).isEqualTo(1L),
                () -> assertThat(result.get(0).nickname()).isEqualTo(member1.getNickname()),
                () -> assertThat(result.get(1).memberId()).isEqualTo(2L),
                () -> assertThat(result.get(1).profileImage()).isEqualTo(member2.getProfileImage())
        );

        verify(memberGroupRepository, times(1)).findMembersByGroupId(groupId);
    }
}
