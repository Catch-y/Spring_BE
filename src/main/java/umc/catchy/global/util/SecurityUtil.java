package umc.catchy.global.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;

public class SecurityUtil {
    public static Long getCurrentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new GeneralException(ErrorStatus.MEMBER_NOT_FOUND);
        }

        String username = authentication.getName();

        try {
            return Long.valueOf(username);
        } catch (NumberFormatException e) {
            throw new GeneralException(ErrorStatus.MEMBER_NOT_FOUND);
        }
    }
}
