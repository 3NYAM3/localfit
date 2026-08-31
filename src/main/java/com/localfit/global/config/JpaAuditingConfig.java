package com.localfit.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 설정
 *
 * [분리한 이유]
 * 메인 애플리케이션 클래스에 @EnableJpaAuditing을 붙이면 @WebMvcTest처럼 JPA를 로딩하지 않는 슬라이스 테스트에서도 이 설정이 적용되어
 * "JPA metamodel must not be empty" 오류가 발생한다.
 */

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
