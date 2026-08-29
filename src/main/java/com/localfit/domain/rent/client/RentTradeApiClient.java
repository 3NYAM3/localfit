package com.localfit.domain.rent.client;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.localfit.domain.rent.dto.RentApiResponse;
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

/**
 * 국토교통부 아파트 전월세 실거래가 호출 클라이언트
 * 응답형식 XML
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RentTradeApiClient {

    private static final String BASE_URL = "https://apis.data.go.kr/1613000/RTMSDataSvcAptRent/getRTMSDataSvcAptRent";

    private final RestClient.Builder restClientBuilder;
    private final PublicDataProperties publicDataProperties;
    private final XmlMapper xmlMapper = new XmlMapper();

    /**
     * 아파트 전월세 실거래 목록을 조회
     * @param lawdCd    시군구 법정도코드 앞 5자리
     * @param dealYmd   거래년월 (yyyymm)
     * @param pageNo    페이지 번호
     * @param numOfRows 한 페이지 결과 수
     * @return  파싱된 전월세 실거래 응답
     */
    public RentApiResponse fetch(String lawdCd, String dealYmd, int pageNo, int numOfRows) {
        RestClient restClient = restClientBuilder.build();
        String finalUri = buildUri(lawdCd, dealYmd, pageNo, numOfRows);

        // String으로 원문을 받음
        String rawResponse = restClient.get()
                .uri(URI.create(finalUri))
                .retrieve()
                .body(String.class);

        // XML형식인지 검증
        // XML은 '<'로 시작 - HTML 오류 페이지도 '<'로 시작하지만 파싱 단계에서 걸러진다
        if (rawResponse == null || !rawResponse.trim().startsWith("<")) {
            log.error("[RentTradeApiClient] XML 아닌 응답 - lawdCd: {}, dealYmd: {}, 응답: {}",
                    lawdCd, dealYmd, truncate(rawResponse));
            throw new CustomException(ErrorCode.EXTERNAL_API_INVALID_RESPONSE);
        }

        // DTO파싱
        try {
            return xmlMapper.readValue(rawResponse, RentApiResponse.class);
        } catch (Exception e) {
            log.error("[RentTradeApiClient] XML 파싱 실패 - lawdCd: {}, dealYmd: {}, 응답: {}",
                    lawdCd, dealYmd, truncate(rawResponse), e);
            throw new CustomException(ErrorCode.EXTERNAL_API_INVALID_RESPONSE);
        }
    }

    /**
     * URI조립
     * 서비스키를 미리 디코딩한 뒤 다른 파라미터와 함께 넣고 전체를 한 번에 인코딩해 이중 인코딩 문제를 방지한다.
     */
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
    /** 로그가 너무 길어지지 않도록 응답 원문을 300자로 잘라서 반환 */
    private String truncate(String text) {
        if (text == null) return "null";
        return text.length() > 300 ? text.substring(0, 300) + "..." : text;
    }
}
