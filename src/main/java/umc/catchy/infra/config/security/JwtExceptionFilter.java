package umc.catchy.infra.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import umc.catchy.global.common.response.BaseResponse;
import umc.catchy.global.common.response.code.ErrorReasonDTO;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;

@Component
@RequiredArgsConstructor
public class JwtExceptionFilter extends OncePerRequestFilter {
    private final ObjectMapper jacksonObjectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (GeneralException e) {
            ErrorReasonDTO errorReason = e instanceof GeneralException
                    ? ((GeneralException) e).getErrorReason()
                    : null;

            BaseResponse<?> baseResponse;

            if (errorReason != null) {
                if (ErrorStatus.INVALID_TOKEN.getMessage().equals(errorReason.getMessage())) {
                    baseResponse = BaseResponse.onFailure(ErrorStatus.INVALID_TOKEN);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
                } else if (ErrorStatus.TOKEN_EXPIRED.getMessage().equals(errorReason.getMessage())) {
                    baseResponse = BaseResponse.onFailure(ErrorStatus.TOKEN_EXPIRED);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
                } else if (ErrorStatus.UNSUPPORTED_TOKEN.getMessage().equals(errorReason.getMessage())) {
                    baseResponse = BaseResponse.onFailure(ErrorStatus.UNSUPPORTED_TOKEN);
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
                } else if (ErrorStatus.NOT_FOUND_TOKEN.getMessage().equals(errorReason.getMessage())) {
                    baseResponse = BaseResponse.onFailure(ErrorStatus.NOT_FOUND_TOKEN);
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
                } else {
                    baseResponse = BaseResponse.onFailure(ErrorStatus._UNAUTHORIZED);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
                }
            } else {
                baseResponse = BaseResponse.onFailure(ErrorStatus._UNAUTHORIZED);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
            }

            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(jacksonObjectMapper.writeValueAsString(baseResponse));
        }
    }
}