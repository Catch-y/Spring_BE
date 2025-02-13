package umc.catchy.infra.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import umc.catchy.domain.jwt.service.BlackTokenRedisService;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.JwtUtil;


@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;
    private final BlackTokenRedisService blackTokenRedisService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);

        if (token != null && jwtUtil.validateToken(token)) {
            // 블랙리스트 토큰인지 검사
            if (blackTokenRedisService.isTokenBlacklisted(token)) {
                throw new GeneralException(ErrorStatus.BLACKLISTED_TOKEN);
            }

            Long memberId = jwtUtil.getMemberIdFromToken(token);

            if (!memberRepository.existsById(memberId)) {
                throw new GeneralException(ErrorStatus.MEMBER_NOT_FOUND);
            }

            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(memberId, null, null);

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        }

        log.info("Request to {} passed through JwtAuthenticationFilter", request.getRequestURI());

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}
