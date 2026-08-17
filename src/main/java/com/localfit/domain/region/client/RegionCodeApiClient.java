package com.localfit.domain.region.client;

import com.localfit.domain.region.config.RegionSyncProperties;
import com.localfit.domain.region.dto.RegionCodeApiResponse;
import com.localfit.global.config.PublicDataProperties;
import com.localfit.global.exception.CustomException;
import com.localfit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class RegionCodeApiClient {

    private static final String BASE_URL = "https://apis.data.go.kr/1741000/StanReginCd/getStanReginCdList";

    private final RestClient.Builder restClientBuilder;
    private final PublicDataProperties publicDataProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RegionCodeApiResponse fetchByAddressKeyword(String keyword, int pageNo, int numOfRows) {
        RestClient restClient = restClientBuilder.build();
        String finalUri = buildUri(keyword, pageNo, numOfRows);

        // String으로 원문을 받는다 (Content-Type이 HTML이어도 예외 없이 받아짐)
        String rawResponse = restClient.get()
                .uri(URI.create(finalUri))
                .retrieve()
                .body(String.class);

        // JSON 형식인지 간단히 검증 (HTML 오류 페이지 방어)
        if (rawResponse == null || !rawResponse.trim().startsWith("{")) {
            log.error("[RegionCodeApiClient] JSON이 아닌 응답 수신 - keyword: {}, 응답: {}",
                    keyword, truncate(rawResponse));
            throw new CustomException(ErrorCode.EXTERNAL_API_INVALID_RESPONSE);
        }

        // 수동으로 DTO 파싱
        try {
            return objectMapper.readValue(rawResponse, RegionCodeApiResponse.class);
        } catch (Exception e) {
            log.error("[RegionCodeApiClient] JSON 파싱 실패 - keyword: {}, 응답: {}",
                    keyword, truncate(rawResponse), e);
            throw new CustomException(ErrorCode.EXTERNAL_API_INVALID_RESPONSE);
        }
    }

    //URI 조립
    private String buildUri(String keyword, int pageNo, int numOfRows) {
        String baseUri = UriComponentsBuilder
                .fromUriString(BASE_URL)
                .queryParam("type", "json")
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", numOfRows)
                .queryParam("flag", "Y")
                .queryParam("locatadd_nm", keyword)
                .encode(StandardCharsets.UTF_8)         // encode() → build() 순서 (이중 인코딩 방지)
                .build()
                .toUriString();

        return baseUri + "&ServiceKey=" + publicDataProperties.getServiceKey();
    }

    //로그가 너무 길어지지 않도록 응답 원문을 300자로 잘라서 반환
    private String truncate(String text) {
        if (text == null) return "null";
        return text.length() > 300 ? text.substring(0, 300) + "..." : text;
    }
}
