package umc.catchy.domain.vote.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import umc.catchy.domain.mapping.memberGroup.dao.MemberGroupRepository;
import umc.catchy.domain.vote.domain.Vote;
import umc.catchy.domain.vote.dto.request.CategoryVoteRequest;
import umc.catchy.domain.vote.dto.request.VoteCreateRequest;
import umc.catchy.infra.config.fcm.FCMService;

import java.util.List;

import static umc.catchy.global.common.constants.FcmConstants.*;

@Component
@RequiredArgsConstructor
public class VoteFacade {

    private final VoteCommandService voteCommandService;
    private final MemberGroupRepository memberGroupRepository;
    private final FCMService fcmService;

    public Long createVote(VoteCreateRequest request) {
        Vote vote = voteCommandService.createVote(request.groupId());
        List<String> deviceTokens = getGroupDeviceTokens(request.groupId());
        fcmService.sendGroupMessageAsync(deviceTokens, COURSE_UPDATED_MESSAGE_TITLE, GROUP_VOTE_START_MESSAGE_CONTENT);
        return vote.getId();
    }

    public void submitVote(Long voteId, CategoryVoteRequest request, Long memberId) {
        voteCommandService.submitVote(voteId, request.categoryIds(), memberId);
    }

    public void revoteCategory(Long voteId, CategoryVoteRequest request, Long memberId) {
        voteCommandService.revoteCategory(voteId, request.categoryIds(), memberId);
    }

    private List<String> getGroupDeviceTokens(Long groupId) {
        return memberGroupRepository.findMembersByGroupId(groupId).stream()
                .filter(member -> member.getFcmInfo().getAppAlarm())
                .map(member -> member.getFcmInfo().getFcmToken())
                .toList();
    }
}
