package com.localfit.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

/**
 * RestClient 공통 설정
 * 모든 공공데이터API 클라이언트가 이 Builder를 주입받아 사용
 * 일부 공공데이터API가 비브라우저요청을 차단하는경우가 있어 방어적으로 설정
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
