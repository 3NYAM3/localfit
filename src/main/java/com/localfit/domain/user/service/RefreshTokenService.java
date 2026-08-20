package com.localfit.domain.user.service;

import com.localfit.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh:";

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    public void save(Long userId, String refreshToken) {
        redisTemplate.opsForValue().set(
                buildKey(userId),
                refreshToken,
                Duration.ofSeconds(jwtTokenProvider.getRefreshTokenValiditySeconds())
        );
    }

    public boolean isValid(Long userId, String refreshToken){
        String saved = redisTemplate.opsForValue().get(buildKey(userId));

        if(saved == null || !saved.equals(refreshToken)){
            log.warn("[RefreshToken] userId={} 토큰 불일치 - 재사용 의심으로 세션 폐기", userId);
            delete(userId);
            return false;
        }
        return true;
    }

    public void delete(Long userId){
        redisTemplate.delete(buildKey(userId));
    }

    public String buildKey(Long userId){
        return KEY_PREFIX + userId;
    }
}
