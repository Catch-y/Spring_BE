package umc.catchy.domain.vote.dto.response.group;

import umc.catchy.domain.vote.dto.response.category.CategoryResult;
import java.util.List;

public record GroupVoteResultResponse(String groupLocation, List<CategoryResult> categories) {}
