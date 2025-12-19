package umc.catchy.domain.mapping.placeCourse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlaceSearchRequest(
        @NotNull(message = "poiId는 필수입니다")
        Long poiId,

        @NotNull(message = "위도는 필수입니다")
        Double latitude,

        @NotNull(message = "경도는 필수입니다")
        Double longitude,

        @NotBlank(message = "장소명은 필수입니다")
        String placeName,

        String address
) {
}