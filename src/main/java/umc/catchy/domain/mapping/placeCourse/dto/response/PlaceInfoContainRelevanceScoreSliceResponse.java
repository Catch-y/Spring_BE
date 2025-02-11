package umc.catchy.domain.mapping.placeCourse.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Slice;

import java.util.List;

public record PlaceInfoContainRelevanceScoreSliceResponse(
        @Schema(description = "장소(검색어 가중치를 더한) 데이터") List<PlaceInfoContainRelevance> content,
        @Schema(description = "마지막 페이지 여부") Boolean last){

        public static PlaceInfoContainRelevanceScoreSliceResponse from(Slice<PlaceInfoContainRelevance> placeInfoResponses) {
                return new PlaceInfoContainRelevanceScoreSliceResponse(placeInfoResponses.getContent(),placeInfoResponses.isLast());
    }
}
