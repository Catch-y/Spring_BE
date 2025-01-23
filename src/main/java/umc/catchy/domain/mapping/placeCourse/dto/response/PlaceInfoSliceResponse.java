package umc.catchy.domain.mapping.placeCourse.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Slice;

import java.util.List;

public record PlaceInfoSliceResponse(
        @Schema(description = "장소 데이터") List<PlaceInfoResponse> content,
        @Schema(description = "마지막 페이지 여부") Boolean last){

        public static PlaceInfoSliceResponse from(Slice<PlaceInfoResponse> placeInfoResponses) {
                return new PlaceInfoSliceResponse(placeInfoResponses.getContent(),placeInfoResponses.isLast());
    }
}
