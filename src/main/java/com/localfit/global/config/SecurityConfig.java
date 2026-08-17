package com.localfit.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정.
 *
 * [현재 상태] 개발 초기 임시 설정 - 모든 요청을 인증 없이 허용.
 * Spring Security 의존성이 프로젝트에 포함된 순간부터, 이 설정이 없으면
 * 기본적으로 모든 API가 인증을 요구하게 되어 개발이 막힌다.
 *
 * [예정] Phase 2(JWT 인증 구현)에서 아래처럼 세분화할 예정:
 * - /api/auth/**  → 인증 없이 허용 (회원가입/로그인)
 * - /api/regions/** → 인증 없이 허용 (지역 조회는 공개 API)
 * - /api/favorites/** → 인증 필요 (내 관심지역 등 개인 데이터)
 */

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}
