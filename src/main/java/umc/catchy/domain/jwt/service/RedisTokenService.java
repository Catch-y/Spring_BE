package umc.catchy.domain.jwt.service;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisTokenService {
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${cache.refreshToken.key}")
    private String REFRESH_TOKEN_PREFIX;

    @Value("${cache.refreshToken.ttl}")
    private long REFRESH_TOKEN_TTL;

    // 리프레시 토큰 저장
    public void saveRefreshToken(String refreshToken, Long memberId) {
        if (refreshToken == null || memberId == null) {
            throw new GeneralException(ErrorStatus.INVALID_TOKEN);
        }

        String key = REFRESH_TOKEN_PREFIX + ":" + memberId;
        try {
            redisTemplate.opsForValue().set(key, refreshToken, REFRESH_TOKEN_TTL, TimeUnit.SECONDS);
            log.debug("RefreshToken saved for user: {}", memberId);
        } catch (Exception e) {
            log.error("Failed to save refresh token for user: {}", memberId, e);
            throw new GeneralException(ErrorStatus.TOKEN_STORAGE_FAILED);
        }
    }

    // 이메일 조회
    public String getRefreshTokenByMemberId(Long memberId) {
        if (memberId == null) {
            return null;
        }

        String key = REFRESH_TOKEN_PREFIX + ":" + memberId;

        return redisTemplate.opsForValue().get(key);
    }

    // 리프레시 토큰 유효성 검사
    public boolean isRefreshTokenValid(String refreshToken, Long memberId) {
        String stored = getRefreshTokenByMemberId(memberId);
        return refreshToken != null && refreshToken.equals(stored);
    }

    // 리프레시 토큰 삭제
    public void deleteRefreshTokenByMemberId(Long memberId) {
        if (memberId == null) {
            return;
        }

        String key = REFRESH_TOKEN_PREFIX + ":" + memberId;

        try {
            Boolean deleted = redisTemplate.delete(key);
            log.debug("RefreshToken deletion result for memberId {}: {}", memberId, deleted);
        } catch (Exception e) {
            log.error("Failed to delete refresh token for memberId: {}", memberId, e);
        }
    }
}
