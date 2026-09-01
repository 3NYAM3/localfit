package com.localfit.global.config;

import com.localfit.global.common.ApiResponse;
import com.localfit.global.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Spring Security 설정.
 */

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        // 인증 실패(토큰 없음/무효) 시 403 대신 401을 명시적으로 반환
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setCharacterEncoding("UTF-8");
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setStatus(401);

                            response.getWriter().write(
                                    objectMapper.writeValueAsString(
                                            ApiResponse.error(401, "인증이 필요합니다.")));
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        // 인증 불필요 - 공개 API
                        .requestMatchers("/api/auth/signup", "/api/auth/signin", "/api/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/regions/**").permitAll()
                        .requestMatchers("/internal/**").permitAll()// 개발용 동기화 API - 추후 ADMIN 권한으로 제한 예정
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll() //Swagger
                        // 나머지는 인증 필요 (signout, me 등)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
