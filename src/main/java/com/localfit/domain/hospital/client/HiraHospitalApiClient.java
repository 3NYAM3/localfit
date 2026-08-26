package com.localfit.domain.hospital.client;

import com.localfit.domain.hospital.dto.HiraHospitalResponse;
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * HIRA 병원정보서비스 호출 클라이언트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HiraHospitalApiClient {

    private static final String BASE_URL = "http://apis.data.go.kr/B551182/hospInfoServicev2/getHospBasisList";
    private static final Charset EUC_KR = Charset.forName("EUC-KR");

    private final RestClient.Builder restClientBuilder;
    private final PublicDataProperties publicDataProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HiraHospitalResponse fetch(String sidoCd, String clCd, int pageNo, int numOfRows) {
        RestClient restClient = restClientBuilder.build();
        String finalUri = buildUri(sidoCd, clCd,  pageNo, numOfRows);

        String rawResponse = restClient.get()
                .uri(URI.create(finalUri))
                .retrieve()
                .body(String.class);

        if (rawResponse == null || !rawResponse.trim().startsWith("{")) {
            log.error("[HiraHospitalApiClient] JSON이 아닌 응답 - sidoCd: {}, 응답: {}", sidoCd, truncate(rawResponse));
            throw new CustomException(ErrorCode.EXTERNAL_API_INVALID_RESPONSE);
        }

        try {
            return objectMapper.readValue(rawResponse, HiraHospitalResponse.class);
        } catch (Exception e) {
            log.error("[HiraHospitalApiClient] JSON 파싱 실패 - sidoCd: {}", sidoCd, e);
            throw new CustomException(ErrorCode.EXTERNAL_API_INVALID_RESPONSE);
        }
    }

    /** URI조립 */
    private String buildUri(String sidoCd, String clCd, int pageNo, int numOfRows) {
        String decodedKey = URLDecoder.decode(publicDataProperties.getServiceKey(), StandardCharsets.UTF_8);

        return UriComponentsBuilder
                .fromUriString(BASE_URL)
                .queryParam("serviceKey", decodedKey)
                .queryParam("sidoCd", sidoCd)
                .queryParam("clCd", clCd)
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", numOfRows)
                .queryParam("_type", "json")   // JSON 응답 요청
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();
    }

    /** 로그가 너무 길어지지 않도록 응답 원문을 300자로 잘라서 반환 */
    private String truncate(String text) {
        if (text == null) return "null";
        return text.length() > 300 ? text.substring(0, 300) + "..." : text;
    }
}
