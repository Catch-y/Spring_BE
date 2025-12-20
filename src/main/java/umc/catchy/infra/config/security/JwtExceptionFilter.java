package umc.catchy.infra.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import umc.catchy.global.common.response.BaseResponse;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;

import java.io.IOException;

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
            ErrorStatus errorStatus = e.getErrorStatus();
            BaseResponse<?> baseResponse = BaseResponse.onFailure(errorStatus);

            response.setStatus(errorStatus.getHttpStatus().value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(jacksonObjectMapper.writeValueAsString(baseResponse));
        }
    }
}