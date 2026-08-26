package com.localfit.domain.hospital.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * HIRA 병원정보서비스의 XML응답 매핑 DTO
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class HiraHospitalResponse {

    @JsonProperty("response")
    private ResponseBody response;

    public String getResultCode() {
        return response == null ? null : response.header.resultCode;
    }

    public String getResultMessage() {
        return response == null ? null : response.header.resultMsg;
    }

    public List<HospitalItem> getItems() {
        if (response == null || response.body == null || response.body.items == null) {
            return List.of();
        }
        return response.body.items.item == null ? List.of() : response.body.items.item;
    }

    public int getTotalCount() {
        return response == null || response.body == null ? 0 : response.body.totalCount;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResponseBody {
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
        /**
         * 결과가 1건일 때 배열이 아닌 단일 객체로 올 수 있어 방어적으로 처리.
         */
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        private List<HospitalItem> item;
    }

    /**
     * 병원 1건 (필요한 필드만 추림, 진료과목별 인원수 등은 제외)
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HospitalItem {
        private String ykiho;

        @JsonProperty("yadmNm")
        private String hospitalName;

        @JsonProperty("clCd")
        private String clCode;

        @JsonProperty("clCdNm")
        private String clCodeName;

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
