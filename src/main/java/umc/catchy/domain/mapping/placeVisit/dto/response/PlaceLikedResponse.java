package umc.catchy.domain.mapping.placeVisit.dto.response;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PlaceLikedResponse {
    private Long placeVisitId;
    private boolean liked;
}
