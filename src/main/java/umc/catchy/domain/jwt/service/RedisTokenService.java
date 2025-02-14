package umc.catchy.domain.jwt.service;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisTokenService {
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${cache.refreshToken.key}")
    private String REDIS_REFRESH_TOKEN_KEY_PREFIX;

    @Value("${cache.refreshToken.ttl}")
    private long EXPIRATION;

    // 리프레시 토큰 저장
    public void addRefreshToken(String refreshToken, Long memberId) {
        String key = REDIS_REFRESH_TOKEN_KEY_PREFIX + memberId;
        redisTemplate.opsForValue().set(key, refreshToken, EXPIRATION, TimeUnit.SECONDS);
    }

    // 리프레시 토큰 유효성 검사
    public boolean isRefreshTokenValid(String refreshToken, Long memberId) {
        String key = REDIS_REFRESH_TOKEN_KEY_PREFIX + memberId;
        String storedToken = redisTemplate.opsForValue().get(key);
        return storedToken != null && storedToken.equals(refreshToken);
    }

    // 리프레시 토큰 삭제
    public void deleteRefreshToken(Long memberId) {
        String key = REDIS_REFRESH_TOKEN_KEY_PREFIX + memberId;
        redisTemplate.delete(key);
    }

    // 리프레시 토큰 찾기
    public String getRefreshToken(Long memberId) {
        String key = REDIS_REFRESH_TOKEN_KEY_PREFIX + memberId;
        return redisTemplate.opsForValue().get(key);
    }
}
