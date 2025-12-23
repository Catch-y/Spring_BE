package umc.catchy.domain.group.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.catchy.domain.group.dto.response.GroupCalendarResponse;
import umc.catchy.domain.mapping.memberGroup.dao.MemberGroupRepository;
import umc.catchy.domain.mapping.memberGroup.domain.MemberGroup;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.support.fixture.GroupFixture;
import umc.catchy.support.fixture.MemberFixture;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupQueryServiceTest {

    @InjectMocks
    private GroupQueryService groupQueryService;

    @Mock
    private MemberGroupRepository memberGroupRepository;

    @Mock
    private MemberRepository memberRepository;

    @Test
    @DisplayName("사용자 그룹 달력 조회 검증")
    void getUserGroups_success() {
        Long memberId = 1L;
        Member testMember = MemberFixture.createTestMember(memberId, "test@test.com");
        MemberGroup mg = MemberGroup.create(GroupFixture.createGroupWithId(100L, "그룹", "C1"), testMember);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(testMember));
        when(memberGroupRepository.findAllByMemberIdAndDateRange(anyLong(), any(), any()))
                .thenReturn(List.of(mg));

        List<GroupCalendarResponse> result = groupQueryService.getUserGroups(memberId, 2025, 1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).groupName()).isEqualTo("그룹");
    }
}
