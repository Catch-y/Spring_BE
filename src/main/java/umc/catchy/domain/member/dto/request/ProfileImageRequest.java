package umc.catchy.domain.member.dto.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record ProfileImageRequest(
        @NotNull(message = "변경할 프로필 사진을 선택해주세요.")
        MultipartFile profileImage
) {
}
