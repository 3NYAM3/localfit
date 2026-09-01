package com.localfit.domain.hospital.service;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * HIRA 응답의 시군구명을 Region.sigungu 표기와 일치하도록 정규화하는 컴포넌트
 */
@Component
public class HiraRegionNormalizer {

    /** 경기도 일반구 표기를 시 단위로 병합하기 위한 매핑 */
    private static final Map<String, String> GYEONGGI_DISTRICT_TO_CITY = Map.ofEntries(
            Map.entry("부천원미구", "부천시"),
            Map.entry("부천소사구", "부천시"),
            Map.entry("부천오정구", "부천시"),
            Map.entry("성남수정구", "성남시"),
            Map.entry("성남중원구", "성남시"),
            Map.entry("성남분당구", "성남시"),
            Map.entry("수원권선구", "수원시"),
            Map.entry("수원장안구", "수원시"),
            Map.entry("수원팔달구", "수원시"),
            Map.entry("수원영통구", "수원시"),
            Map.entry("안양만안구", "안양시"),
            Map.entry("안양동안구", "안양시"),
            Map.entry("안산단원구", "안산시"),
            Map.entry("안산상록구", "안산시"),
            Map.entry("고양덕양구", "고양시"),
            Map.entry("고양일산서구", "고양시"),
            Map.entry("고양일산동구", "고양시"),
            Map.entry("용인기흥구", "용인시"),
            Map.entry("용인수지구", "용인시"),
            Map.entry("용인처인구", "용인시"),
            Map.entry("화성병점구", "화성시"),
            Map.entry("화성만세구", "화성시"),
            Map.entry("화성동탄구", "화성시"),
            Map.entry("화성효행구", "화성시")
    );

    /**
     * HIRA의 시군구명을 Region 표기에 맞게 정규화한다.
     *
     * @param sidoName    HIRA 시도명
     * @param sigunguName HIRA 시군구명
     * @return 정규화된 시군구명
     */
    public String normalizeSigungu(String sidoName, String sigunguName) {
        if (sigunguName == null) {
            return null;
        }

        if ("인천".equals(sidoName)) {
            return sigunguName.replaceFirst("^인천", "");
        }

        if ("경기".equals(sidoName)) {
            return GYEONGGI_DISTRICT_TO_CITY.getOrDefault(sigunguName, sigunguName);
        }

        return sigunguName;   // 서울은 그대로
    }

    /** HIRA 시도명을 Region.sido 표기로 변환 */
    public String normalizeSido(String hiraSidoName) {
        return switch (hiraSidoName) {
            case "서울" -> "서울특별시";
            case "인천" -> "인천광역시";
            case "경기" -> "경기도";
            default -> hiraSidoName;
        };
    }

}
