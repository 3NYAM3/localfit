package com.localfit.domain.rent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;

import java.util.List;

@Getter
@JacksonXmlRootElement(localName = "response")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RentTradeApiResponse {

    @JacksonXmlProperty(localName = "header")
    private Header header;

    @JacksonXmlProperty(localName = "body")
    private Body body;

    //결과 코드
    public String getResultCode() {
        return header == null ? null : header.resultCode;
    }

    //결과 메시지
    public String getResultMsg() {
        return header == null ? null : header.resultMsg;
    }

    //실거래 데이터 목록
    public List<Item> getItems() {
        if (body == null || body.items == null || body.items.item == null) {
            return List.of();
        }
        return body.items.item;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        @JacksonXmlProperty(localName = "resultCode")
        private String resultCode;

        @JacksonXmlProperty(localName = "resultMsg")
        private String resultMsg;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        @JacksonXmlProperty(localName = "items")
        private Items items;

        @JacksonXmlProperty(localName = "numOfRows")
        private Integer numOfRows;

        @JacksonXmlProperty(localName = "pageNo")
        private Integer pageNo;

        @JacksonXmlProperty(localName = "totalCount")
        private Integer totalCount;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items {
        @JacksonXmlProperty(localName = "item")
        @JacksonXmlElementWrapper(useWrapping = false)  // <item>이 반복되는 구조라 래핑 없이 리스트 매핑
        private List<Item> item;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        @JacksonXmlProperty(localName = "sggCd")
        private String sggCd;          // 지역코드 5자리

        @JacksonXmlProperty(localName = "umdNm")
        private String umdNm;          // 법정동명

        @JacksonXmlProperty(localName = "aptNm")
        private String aptNm;          // 아파트명

        @JacksonXmlProperty(localName = "excluUseAr")
        private String excluUseAr;     // 전용면적(㎡)

        @JacksonXmlProperty(localName = "dealYear")
        private String dealYear;

        @JacksonXmlProperty(localName = "dealMonth")
        private String dealMonth;

        @JacksonXmlProperty(localName = "dealDay")
        private String dealDay;

        @JacksonXmlProperty(localName = "deposit")
        private String deposit;        // 보증금 - 콤마 포함 문자열 (예: "29,768")

        @JacksonXmlProperty(localName = "monthlyRent")
        private String monthlyRent;    // 월세 - 콤마 포함 문자열

        @JacksonXmlProperty(localName = "floor")
        private String floor;

        @JacksonXmlProperty(localName = "buildYear")
        private String buildYear;
    }
}
