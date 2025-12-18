package umc.catchy.global.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Slice;

import java.util.List;

public record SliceResponse<T>(
        @Schema(description = "데이터 목록")
        List<T> content,

        @Schema(description = "마지막 페이지 여부")
        Boolean isLast
) {
    public static <T> SliceResponse<T> of(Slice<T> slice) {
        return new SliceResponse<>(
                slice.getContent(),
                slice.isLast()
        );
    }
}
