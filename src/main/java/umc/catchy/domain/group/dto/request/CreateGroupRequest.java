package umc.catchy.domain.group.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Getter
@Setter
public class CreateGroupRequest {

    @Schema(description = "그룹 이름", example = "Study Group", required = true)
    @NotBlank
    private String groupName;

    @Schema(description = "그룹 위치", example = "서울시 강남구", required = true)
    @NotBlank
    private String groupLocation;

    @Schema(description = "약속 시간 (LocalDateTime 형식)", example = "2025-01-25T15:30:00", required = true)
    @NotNull
    private LocalDateTime promiseTime;

    @Schema(description = "초대 코드", example = "ABC123", required = true)
    @NotBlank
    private String inviteCode;

    @Schema(description = "그룹 이미지 파일")
    private MultipartFile groupImage;
}