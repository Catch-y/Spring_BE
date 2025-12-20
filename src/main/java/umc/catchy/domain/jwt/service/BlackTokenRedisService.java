package umc.catchy.domain.jwt.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import umc.catchy.global.util.JwtUtil;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class BlackTokenRedisService {
    private final RedisTemplate<String, String> redisTemplate;
    private final JwtUtil jwtUtil;

    @Value("${cache.blacklist.key}")
    private String BLACKLIST_PREFIX;

    // 블랙리스트 토큰 저장
    public void addBlacklistedToken(String token) {
        if (token == null) {
            return;
        }

        try {
            Date expirationTime = jwtUtil.getExpirationTime(token);
            long currentTime = System.currentTimeMillis();
            long ttl = expirationTime.getTime() - currentTime;

            if (ttl <= 0) {
                log.debug("Token already expired, not adding to blacklist");
                return;
            }

            String key = BLACKLIST_PREFIX + ":" + token;

            redisTemplate.opsForValue().set(key, "blacklisted", ttl, TimeUnit.MILLISECONDS);
            log.debug("Added token to blacklist with TTL: {} ms", ttl);
        } catch (Exception e) {
            log.error("Failed to add token to blacklist", e);
        }
    }

    // 블랙리스트 토큰 유효성 검사
    public boolean isTokenBlacklisted(String token) {
        if (token == null) {
            return false;
        }

        String key = BLACKLIST_PREFIX + ":" + token;

        try {
            Boolean exists = redisTemplate.hasKey(key);
            log.debug("Token blacklist check result: {}", exists);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Failed to check token blacklist status", e);
            return true;
        }
    }

    // 블랙리스트 토큰 제거
    public void removeTokenFromBlacklist(String token) {
        if (token == null) {
            return;
        }

        String key = BLACKLIST_PREFIX + ":" + token;

        try {
            Boolean deleted = redisTemplate.delete(key);
            log.debug("Token removal from blacklist result: {}", deleted);
        } catch (Exception e) {
            log.error("Failed to remove token from blacklist", e);
        }
    }
}
