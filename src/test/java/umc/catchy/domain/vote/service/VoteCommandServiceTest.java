package umc.catchy.domain.vote.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.categoryVote.dao.CategoryVoteRepository;
import umc.catchy.domain.group.dao.GroupRepository;
import umc.catchy.domain.group.domain.Groups;
import umc.catchy.domain.mapping.memberCategoryVote.dao.MemberCategoryVoteRepository;
import umc.catchy.domain.mapping.memberGroup.dao.MemberGroupRepository;
import umc.catchy.domain.mapping.memberPlaceVote.dao.MemberPlaceVoteRepository;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.dao.PlaceRepository;
import umc.catchy.domain.vote.dao.VoteRepository;
import umc.catchy.domain.vote.domain.Vote;
import umc.catchy.domain.vote.domain.VoteStatus;
import umc.catchy.domain.vote.dto.request.PlaceVoteRequest;
import umc.catchy.support.fixture.GroupFixture;
import umc.catchy.support.fixture.VoteFixture;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoteCommandServiceTest {

    @InjectMocks
    private VoteCommandService voteCommandService;

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private CategoryVoteRepository categoryVoteRepository;

    @Mock
    private MemberCategoryVoteRepository memberCategoryVoteRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberGroupRepository memberGroupRepository;

    @Mock
    private MemberPlaceVoteRepository memberPlaceVoteRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Test
    @DisplayName("투표 생성 성공 - 모든 카테고리 저장 확인")
    void createVote_success() {
        Long groupId = 1L;
        Groups group = GroupFixture.createGroupWithId(groupId, "그룹", "ABC");
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(voteRepository.save(any(Vote.class))).thenReturn(VoteFixture.createVoteWithId(100L, group));

        voteCommandService.createVote(groupId);

        verify(categoryVoteRepository, times(BigCategory.values().length)).save(any());
        verify(voteRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("투표 완료 상태 업데이트 확인")
    void checkAndUpdateVoteCompletion_success() {
        Long voteId = 100L;
        Vote vote = VoteFixture.createVoteWithId(voteId, GroupFixture.createGroupWithId(1L, "G", "A"));
        when(voteRepository.findById(voteId)).thenReturn(Optional.of(vote));
        when(memberGroupRepository.countByGroupId(any())).thenReturn(5);
        when(memberCategoryVoteRepository.countDistinctMembersByVoteId(voteId)).thenReturn(5);

        voteCommandService.checkAndUpdateVoteCompletion(voteId);

        assertThat(vote.getStatus()).isEqualTo(VoteStatus.COMPLETED);
    }

    @Test
    @DisplayName("장소 투표 토글 - 추가 로직 확인")
    void togglePlaceVote_add() {
        PlaceVoteRequest request = new PlaceVoteRequest(50L);
        when(memberRepository.findById(any())).thenReturn(Optional.of(mock(Member.class)));
        when(placeRepository.findById(50L)).thenReturn(Optional.of(mock(umc.catchy.domain.place.domain.Place.class)));
        when(voteRepository.findById(any())).thenReturn(Optional.of(mock(Vote.class)));
        when(groupRepository.findById(any())).thenReturn(Optional.of(mock(Groups.class)));
        when(memberPlaceVoteRepository.findByMemberIdAndPlaceIdAndVoteId(any(), any(), any())).thenReturn(null);

        String result = voteCommandService.togglePlaceVote(100L, 1L, request, 1L);

        assertThat(result).isEqualTo("Vote added successfully.");
        verify(memberPlaceVoteRepository, times(1)).save(any());
    }
}
