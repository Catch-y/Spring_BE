package umc.catchy.domain.vote.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlaceVoteResponse {
    private Long memberPlaceVoteId; // 투표 ID
    private String message;         // 성공 메시지
}