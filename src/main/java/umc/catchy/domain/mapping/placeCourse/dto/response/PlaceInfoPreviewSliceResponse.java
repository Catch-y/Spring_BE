package umc.catchy.domain.mapping.placeCourse.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Slice;

public record PlaceInfoPreviewSliceResponse(
        @Schema(description = "장소 데이터") List<PlaceInfoPreview> content,
        @Schema(description = "마지막 페이지 여부") Boolean isLast){

        public static PlaceInfoPreviewSliceResponse from(Slice<PlaceInfoPreview> placeInfoPreviews) {
                return new PlaceInfoPreviewSliceResponse(placeInfoPreviews.getContent(),placeInfoPreviews.isLast());
    }
}
