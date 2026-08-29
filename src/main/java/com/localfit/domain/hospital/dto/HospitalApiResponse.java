package com.localfit.domain.hospital.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * HIRA 병원정보서비스의 JSON응답 매핑 DTO
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class HospitalApiResponse {

    @JsonProperty("response")
    private ResponseWrapper response;

    /** 응답의 결과 코드를 반환 */
    public String getResultCode() {
        return response == null ? null : response.header.resultCode;
    }

    /** 응답의 결과 메시지를 반환 */
    public String getResultMessage() {
        return response == null ? null : response.header.resultMsg;
    }

    /** 응답에 포함된 병원 데이터 목록을 반환 */
    public List<HospitalItem> getItems() {
        if (response == null || response.body == null || response.body.items == null) {
            return List.of();
        }
        return response.body.items.item == null ? List.of() : response.body.items.item;
    }

    /** response 래퍼 - JSON 최상위 "response" 키에 대응 */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResponseWrapper {
        private Header header;
        private Body body;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        private Items items;
        private int numOfRows;
        private int pageNo;
        private int totalCount;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items {
        /**  결과가 1건일 때 배열이 아닌 단일 객체로 올 수 있어 방어적으로 처리. */
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        private List<HospitalItem> item;
    }

    /** 병원 1건 */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HospitalItem {
        private String ykiho;           // 암호화된 요양기관 고유키

        @JsonProperty("yadmNm")
        private String hospitalName;

        @JsonProperty("clCd")
        private String clCode;          // 종별코드

        @JsonProperty("clCdNm")
        private String clCodeName;      // 종별코드명

        @JsonProperty("sidoCdNm")
        private String sidoName;

        @JsonProperty("sgguCdNm")
        private String sigunguName;

        private String addr;

        @JsonProperty("XPos")
        private Double longitude;

        @JsonProperty("YPos")
        private Double latitude;
    }
}