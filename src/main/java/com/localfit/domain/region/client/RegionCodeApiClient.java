package com.localfit.domain.region.client;

import com.localfit.domain.region.dto.RegionApiResponse;
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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * 행정안전부 행정표준코드 법정동코드 호출 클라이언트
 * 응답형식 JSON
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegionCodeApiClient {

    private static final String BASE_URL = "https://apis.data.go.kr/1741000/StanReginCd/getStanReginCdList";

    private final RestClient.Builder restClientBuilder;
    private final PublicDataProperties publicDataProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 법정동코드 목록을 조회
     * @param keyword   검색키워드 (서울특별시, 인천광역시, 경기도)
     * @param pageNo    페이지 번호
     * @param numOfRows 한 페이지 결과 수
     * @return  파싱된 법정동코드 응답
     */
    public RegionApiResponse fetch(String keyword, int pageNo, int numOfRows) {
        RestClient restClient = restClientBuilder.build();
        String finalUri = buildUri(keyword, pageNo, numOfRows);

        // String으로 원문을 받는다
        String rawResponse = restClient.get()
                .uri(URI.create(finalUri))
                .retrieve()
                .body(String.class);

        // JSON 형식인지 간단히 검증
        if (rawResponse == null || !rawResponse.trim().startsWith("{")) {
            log.error("[RegionCodeApiClient] JSON이 아닌 응답 수신 - keyword: {}, 응답: {}", keyword, truncate(rawResponse));
            throw new CustomException(ErrorCode.EXTERNAL_API_INVALID_RESPONSE);
        }

        // DTO 파싱
        try {
            return objectMapper.readValue(rawResponse, RegionApiResponse.class);
        } catch (Exception e) {
            log.error("[RegionCodeApiClient] JSON 파싱 실패 - keyword: {}, 응답: {}",
                    keyword, truncate(rawResponse), e);
            throw new CustomException(ErrorCode.EXTERNAL_API_INVALID_RESPONSE);
        }
    }

    /**
     * URI조립
     * 서비스키를 미리 디코딩한 뒤 다른 파라미터와 함께 넣고 전체를 한 번에 인코딩해 이중 인코딩 문제를 방지한다.
     */
    private String buildUri(String keyword, int pageNo, int numOfRows) {
        String decodedKey = URLDecoder.decode(publicDataProperties.getServiceKey(), StandardCharsets.UTF_8);

        return UriComponentsBuilder
                .fromUriString(BASE_URL)
                .queryParam("serviceKey", decodedKey)
                .queryParam("type", "json")
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", numOfRows)
                .queryParam("flag", "Y")
                .queryParam("locatadd_nm", keyword)
                .encode(StandardCharsets.UTF_8)         // encode() → build() 순서 (이중 인코딩 방지)
                .build()
                .toUriString();
    }

    /**로그가 너무 길어지지 않도록 응답 원문을 300자로 잘라서 반환*/
    private String truncate(String text) {
        if (text == null) return "null";
        return text.length() > 300 ? text.substring(0, 300) + "..." : text;
    }
}
