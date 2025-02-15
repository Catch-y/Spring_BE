package umc.catchy.domain.vote.dto.response.vote;

import java.util.List;

public record VoteResultResponse(String status, int totalMembers, List<VoteResult> results) {}