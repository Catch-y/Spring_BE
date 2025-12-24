package umc.catchy.domain.vote.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.catchy.domain.mapping.memberGroup.dao.MemberGroupRepository;
import umc.catchy.domain.vote.domain.Vote;
import umc.catchy.domain.vote.dto.request.VoteCreateRequest;
import umc.catchy.infra.config.fcm.FCMService;
import umc.catchy.support.fixture.GroupFixture;
import umc.catchy.support.fixture.VoteFixture;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoteFacadeTest {

    @InjectMocks
    private VoteFacade voteFacade;

    @Mock
    private VoteCommandService voteCommandService;

    @Mock
    private MemberGroupRepository memberGroupRepository;

    @Mock
    private FCMService fcmService;


    @Test
    @DisplayName("투표 생성 및 FCM 발송 오케스트레이션 확인")
    void createVote_facade_test() {
        VoteCreateRequest request = new VoteCreateRequest(1L);
        Vote vote = VoteFixture.createVoteWithId(100L, GroupFixture.createGroupWithId(1L, "G", "A"));

        when(voteCommandService.createVote(1L)).thenReturn(vote);
        when(memberGroupRepository.findMembersByGroupId(1L)).thenReturn(List.of());

        voteFacade.createVote(request);

        verify(voteCommandService, times(1)).createVote(1L);
        verify(fcmService, times(1)).sendGroupMessageAsync(anyList(), anyString(), anyString());
    }
}
