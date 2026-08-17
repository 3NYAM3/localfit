package com.localfit.domain.rent.client;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.localfit.domain.rent.dto.RentTradeApiResponse;
import com.localfit.global.config.PublicDataProperties;
import com.localfit.global.exception.CustomException;
import com.localfit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class RentTradeApiClient {

    private static final String BASE_URL = "https://apis.data.go.kr/1613000/RTMSDataSvcAptRent/getRTMSDataSvcAptRent";

    private final RestClient.Builder restClientBuilder;
    private final PublicDataProperties publicDataProperties;
    private final XmlMapper xmlMapper = new XmlMapper();

    public RentTradeApiResponse fetch(String lawdCd, String dealYmd, int pageNo, int numOfRows) {
        RestClient restClient = restClientBuilder.build();
        String finalUri = buildUri(lawdCd, dealYmd, pageNo, numOfRows);

        log.info("[RentTradeApiClient] 요청 URI: {}", finalUri);

        String rawResponse = restClient.get()
                .uri(URI.create(finalUri))
                .retrieve()
                .body(String.class);

        log.info("[RentTradeApiClient] 응답 원문: {}", truncate(rawResponse));

        // XML은 '<'로 시작 - HTML 오류 페이지도 '<'로 시작하지만 파싱 단계에서 걸러진다
        if (rawResponse == null || !rawResponse.trim().startsWith("<")) {
            log.error("[RentTradeApiClient] XML 아닌 응답 - lawdCd: {}, dealYmd: {}, 응답: {}",
                    lawdCd, dealYmd, truncate(rawResponse));
            throw new CustomException(ErrorCode.EXTERNAL_API_INVALID_RESPONSE);
        }

        try {
            return xmlMapper.readValue(rawResponse, RentTradeApiResponse.class);
        } catch (Exception e) {
            log.error("[RentTradeApiClient] XML 파싱 실패 - lawdCd: {}, dealYmd: {}, 응답: {}",
                    lawdCd, dealYmd, truncate(rawResponse), e);
            throw new CustomException(ErrorCode.EXTERNAL_API_INVALID_RESPONSE);
        }
    }

    //URI 조립
    private String buildUri(String lawdCd, String dealYmd, int pageNo, int numOfRows) {
        String decodedKey = URLDecoder.decode(publicDataProperties.getServiceKey(), StandardCharsets.UTF_8);

        return UriComponentsBuilder
                .fromUriString(BASE_URL)
                .queryParam("serviceKey", decodedKey)
                .queryParam("LAWD_CD", lawdCd)
                .queryParam("DEAL_YMD", dealYmd)
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", numOfRows)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();
    }
    //로그가 너무 길어지지 않도록 응답 원문을 300자로 잘라서 반환
    private String truncate(String text) {
        if (text == null) return "null";
        return text.length() > 300 ? text.substring(0, 300) + "..." : text;
    }
}
