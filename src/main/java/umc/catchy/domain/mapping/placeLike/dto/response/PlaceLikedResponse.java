package umc.catchy.domain.mapping.placeLike.dto.response;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PlaceLikedResponse {
    private Long placeLikeId;
    private boolean isLiked;
}
