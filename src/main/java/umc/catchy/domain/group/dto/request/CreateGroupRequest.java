package umc.catchy.domain.group.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Getter
@Setter
public class CreateGroupRequest {
    @NotBlank
    private String groupName;

    @NotBlank
    private String groupLocation;

    @NotNull
    private LocalDateTime promiseTime;

    @NotBlank
    private String inviteCode;

    private MultipartFile groupImage;
}