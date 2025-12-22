package umc.catchy.domain.group.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

public record CreateGroupRequest(
        @Schema(description = "그룹 이름", example = "Study Group", required = true)
        @NotBlank
        String groupName,

        @Schema(description = "그룹 위치", example = "서울시 강남구", required = true)
        @NotBlank
        String groupLocation,

        @Schema(description = "약속 시간 (LocalDateTime 형식)", example = "2025-01-25T15:30:00", required = true)
        @NotNull
        LocalDateTime promiseTime,

        @Schema(description = "초대 코드", example = "ABC123", required = true)
        @NotBlank
        String inviteCode,

        @Schema(description = "그룹 이미지 파일")
        MultipartFile groupImage
) {
}