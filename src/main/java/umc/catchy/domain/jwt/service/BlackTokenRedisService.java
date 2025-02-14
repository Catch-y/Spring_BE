package umc.catchy.domain.jwt.service;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BlackTokenRedisService {
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${cache.blacklist.key}")
    private String REDIS_BLACKLIST_KEY_PREFIX;

    // 블랙리스트 토큰 저장
    public void addBlacklistedToken(String token, long expiration) {
        String key = REDIS_BLACKLIST_KEY_PREFIX + token;
        redisTemplate.opsForValue().set(key, token, expiration, TimeUnit.MILLISECONDS);
    }

    // 블랙리스트 토큰 유효성 검사
    public boolean isTokenBlacklisted(String token) {
        String key = REDIS_BLACKLIST_KEY_PREFIX + token;
        String storedToken = redisTemplate.opsForValue().get(key);
        return storedToken != null && storedToken.equals(token);
    }

    // 블랙리스트 토큰 제거
    public void deleteBlacklistedToken(String token) {
        String key = REDIS_BLACKLIST_KEY_PREFIX + token;
        redisTemplate.delete(key);
    }
}
