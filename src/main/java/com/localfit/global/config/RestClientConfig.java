package com.localfit.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

/**
 * 외부 API 호출에 사용할 RestClient.Builder를 빈으로 등록.
 *
 * Spring Boot가 조건에 따라 이 빈을 자동으로 등록해주기도 하지만,
 * 버전/설정에 따라 자동 등록이 안 되는 경우가 있어 명시적으로 등록한다.
 * 이렇게 등록해두면 각 도메인의 ApiClient(RegionCodeApiClient 등)에서
 * 생성자 주입으로 바로 사용할 수 있다.
 */

/**
 * Java 기본 HttpClient와 Apache HttpClient5는 TLS 핸드셰이크·헤더 인코딩·프로토콜 협상 방식이 미묘하게 달라.
 * 국토교통부 API 게이트웨이가 Java 기본 클라이언트의 요청을 처리하지 못하고 HTTP_ERROR(04)로 반려했던 거지.
 * 브라우저는 성공하는데 코드만 실패하니 URL이나 인증키를 계속 의심하게 되는, 전형적으로 시간 많이 잡아먹는 유형의 문제야.
 * 이건 나중에 면접에서 "외부 API 연동하면서 겪은 문제"로 얘기하기 좋아
 * 요청 URL이 동일한데도 실패해서 헤더 → 인코딩 → HTTP 클라이언트 순으로 변수를 하나씩 통제해가며 원인을 특정했다는 식으로.
 */

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder()
                .defaultHeader("User-Agent", "Mozilla/5.0")
                .defaultHeader("Accept", "*/*");
    }
}
