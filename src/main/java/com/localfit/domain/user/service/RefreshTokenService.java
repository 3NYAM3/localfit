package com.localfit.domain.user.service;

import com.localfit.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis기반 RefreshToken 저장/검증/삭제 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh:";

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * RefreshToken을 Redis에 저장
     *
     * @param userId       사용자ID
     * @param refreshToken 저장할 RefreshToken
     */
    public void save(Long userId, String refreshToken) {
        redisTemplate.opsForValue().set(
                buildKey(userId),
                refreshToken,
                Duration.ofSeconds(jwtTokenProvider.getRefreshTokenValiditySeconds())
        );
    }

    /**
     * 저장된 RefreshToken과 일치하는지 검증
     *
     * @param userId       검증할 사용자ID
     * @param refreshToken 클라이언트가 전달한 RefreshToken
     * @return 유효하면true, 불일치/미존재면 false
     */
    public boolean isValid(Long userId, String refreshToken) {
        String saved = redisTemplate.opsForValue().get(buildKey(userId));

        if (saved == null || !saved.equals(refreshToken)) {
            log.warn("[RefreshToken] userId={} 토큰 불일치 - 재사용 의심으로 세션 폐기", userId);
            delete(userId);
            return false;
        }
        return true;
    }

    /**
     * Redis에서 RefreshToken 삭제 (로그아웃/세선폐기)
     *
     * @param userId 사용자 ID
     */
    public void delete(Long userId) {
        redisTemplate.delete(buildKey(userId));
    }

    /** Redis키 생성 */
    public String buildKey(Long userId) {
        return KEY_PREFIX + userId;
    }
}
