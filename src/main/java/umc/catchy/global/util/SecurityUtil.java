package umc.catchy.global.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;

public class SecurityUtil {
    private static final String BEARER = "Bearer ";

    /* memberId 추출 */
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

    /* refreshToken 추출 */
    public static String extractRefreshToken() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        return request.getHeader("Refresh-Token");
    }

    /* accessToken 추출 */
    public static String getCurrentAccessToken() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith(BEARER)) {
            return authHeader.substring(BEARER.length());
        }

        return null;
    }
}
