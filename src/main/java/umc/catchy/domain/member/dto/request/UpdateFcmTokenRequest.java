package umc.catchy.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class UpdateFcmTokenRequest {

    @Schema(description = "FCM 토큰", defaultValue = "fcm-token-value")
    private String fcmToken;
}
