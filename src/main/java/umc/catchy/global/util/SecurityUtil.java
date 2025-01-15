package umc.catchy.global.util;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;

public class SecurityUtil {
    private static final String BEARER = "Bearer ";

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

    public static String extractRefreshToken() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

        return request.getHeader("Refresh-Token");
    }
}
