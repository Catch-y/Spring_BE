package umc.catchy.domain.mapping.placeCourse.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PlaceInfoPreviewResponse {
    private List<PlaceInfoPreview>  placeInfoPreviews;
    private Boolean isLast;
}
