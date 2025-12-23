package umc.catchy.domain.group.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.catchy.domain.group.dao.GroupRepository;
import umc.catchy.domain.group.domain.Groups;
import umc.catchy.domain.group.dto.request.CreateGroupRequest;
import umc.catchy.domain.mapping.memberGroup.dao.MemberGroupRepository;
import umc.catchy.domain.mapping.memberGroup.domain.MemberGroup;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.support.fixture.GroupFixture;
import umc.catchy.support.fixture.MemberFixture;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupCommandServiceTest {

    @InjectMocks
    private GroupCommandService groupCommandService;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private MemberGroupRepository memberGroupRepository;

    @Mock
    private MemberRepository memberRepository;

    @Test
    @DisplayName("DB 메타데이터 저장 및 멤버 그룹 등록 검증")
    void createGroupMetadata_success() {
        Long memberId = 1L;
        CreateGroupRequest request = GroupFixture.createGroupRequest("테스트 그룹", "ABC1234");
        Member testMember = MemberFixture.createTestMember(memberId, "test@test.com");
        Groups savedGroup = GroupFixture.createGroupWithId(100L, "테스트 그룹", "ABC1234");

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(testMember));
        when(groupRepository.save(any(Groups.class))).thenReturn(savedGroup);

        groupCommandService.createGroupMetadata(request, memberId, "url", LocalDateTime.now());

        verify(groupRepository).save(any(Groups.class));
        verify(memberGroupRepository).save(any(MemberGroup.class));
    }
}
