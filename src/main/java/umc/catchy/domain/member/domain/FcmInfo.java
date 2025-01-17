package umc.catchy.domain.member.domain;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FcmInfo {
    private String fcmToken;
    private Boolean appAlarm;

    public static FcmInfo createFcmInfo() {
        return FcmInfo.builder().appAlarm(true).build();
    }

    public static FcmInfo toggleAlarm(FcmInfo fcmState) {
        return new FcmInfo(fcmState.getFcmToken(), !fcmState.getAppAlarm());
    }

    public static FcmInfo disableAlarm(FcmInfo fcmInfo) {
        return new FcmInfo(fcmInfo.getFcmToken(), false);
    }

    public static FcmInfo deleteToken(FcmInfo fcmInfo) {
        return new FcmInfo("", fcmInfo.getAppAlarm());
    }

    public static FcmInfo updateToken(FcmInfo fcmState, String fcmToken) {
        return new FcmInfo(fcmToken, fcmState.getAppAlarm());
    }
}
