package com.localfit.domain.region.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * 행정안전부 법정동코드 API(StanReginCd)의 JSON 응답을 매핑 DTO
 */

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegionApiResponse {

    @JsonProperty("StanReginCd")
    private List<Block> stanReginCd;

    /** 응답의 결과 코드를 반환 */
    public String getResultCode() {
        if (stanReginCd == null) return null;
        return stanReginCd.stream()
                .filter(block -> block.getHead() != null)
                .flatMap(block -> block.getHead().stream())
                .filter(head -> head.getResult() != null)
                .map(head -> head.getResult().getResultCode())
                .findFirst()
                .orElse(null);
    }

    /** 응답의 결과 메시지를 반환 */
    public String getResultMessage() {
        if (stanReginCd == null) return null;
        return stanReginCd.stream()
                .filter(block -> block.getHead() != null)
                .flatMap(block -> block.getHead().stream())
                .filter(head -> head.getResult() != null)
                .map(head -> head.getResult().getResultMsg())
                .findFirst()
                .orElse(null);
    }

    /** 응답에 포함된 실제 지역 데이터 목록을 반환 */
    public List<Item> getItems() {
        if (stanReginCd == null) return List.of();
        return stanReginCd.stream()
                .filter(block -> block.getRow() != null)
                .flatMap(block -> block.getRow().stream())
                .toList();
    }

    /** StanReginCd 배열의 개별 원소 */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Block {
        private List<Header> head;
        private List<Item> row;
    }

    /** 응답 메타 정보  */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        private Integer totalCount;   // 전체 결과 건수
        private String numOfRows;     // 한 페이지 결과 수 (응답에서 문자열로 옴)
        private String pageNo;        // 현재 페이지 번호 (응답에서 문자열로 옴)
        private String type;          // 응답 문서 형식 (JSON/XML)

        @JsonProperty("RESULT")
        private Result result;        // 처리 결과 코드/메시지
    }

    /** API 처리 결과 코드/메시지 */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private String resultCode;
        private String resultMsg;
    }

    /** 법정동 1건 */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        @JsonProperty("region_cd")
        private String regionCd;          // 법정동코드 10자리 (예: 1171010100)

        @JsonProperty("sido_cd")
        private String sidoCd;            // 시도코드 2자리 (예: 11 = 서울)

        @JsonProperty("sgg_cd")
        private String sggCd;             // 시군구코드 3자리

        @JsonProperty("umd_cd")
        private String umdCd;             // 읍면동코드 3자리

        @JsonProperty("locatadd_nm")
        private String locatAddNm;        // 지역주소명 전체 (예: 서울특별시 송파구 잠실동)

        @JsonProperty("locathigh_cd")
        private String locatHighCd;       // 상위지역코드

        @JsonProperty("locallow_nm")
        private String locallowNm;        // 최하위지역명 (동/리 이름만, 예: 잠실동)
    }
}