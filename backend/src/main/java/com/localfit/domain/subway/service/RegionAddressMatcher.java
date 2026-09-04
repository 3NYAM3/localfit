package com.localfit.domain.subway.service;


import com.localfit.domain.region.entity.Region;
import com.localfit.domain.region.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 도로명주소 문자열에서 시도/시군구를 추출해 Region과 매칭하는 컴포넌트
 *
 * [주소 표기 불일치 대응] 공공데이터는 기관마다, 심지어 같은 파일 안에서도
 * 주소 표기가 제각각이라 여러 단계의 보정이 필요하다.
 * 1. 시도명 축약 ("인천시" → "인천광역시")
 * 2. 시 접미사 누락 ("남양주" → "남양주시")
 * 3. 원본 오타 ("시승시" → "시흥시")
 * 4. 행정구역 개편 미반영 (인천 2026.7.1 개편)
 * 5. 시도/시군구가 아예 없이 동부터 시작하는 주소
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegionAddressMatcher {

    /** 시도명 표기를 우리 Region 표기로 정규화하는 매핑 */
    private static final Map<String, String> SIDO_ALIAS = Map.of(
            "서울시", "서울특별시",
            "서울특별시", "서울특별시",
            "인천시", "인천광역시",
            "인천광역시", "인천광역시",
            "경기", "경기도",
            "경기도", "경기도"
    );

    /**
     * 시군구 표기 보정 매핑.
     * 접미사 누락과 원본 데이터의 오타를 바로잡는다.
     */
    private static final Map<String, String> SIGUNGU_ALIAS = Map.of(
            "남양주", "남양주시",
            "시승시", "시흥시"      // 원본 데이터 오타
    );

    /**
     * 인천 행정구역 개편(2026.7.1) 대응 매핑.
     * 지하철 데이터가 개편 전 표기를 사용하고 있어 신설 구로 변환한다.
     *
     * 서구 → 서해구 / 검단구로 분리
     * 중구 → 제물포구 / 영종구로 분리
     * 동구 → 제물포구로 통합
     * 남구 → 미추홀구 (2018년 명칭 변경)
     *
     * 서구·중구는 분리된 구가 둘이라 시군구명만으로는 판별할 수 없어,
     * 주소에 포함된 지역 키워드로 구분한다.
     */
    private static final Map<String, String> INCHEON_SIMPLE_MAPPING = Map.of(
            "동구", "제물포구",
            "남구", "미추홀구"
    );

    /** 인천 옛 서구 중 검단구로 편입된 지역의 주소 키워드 */
    private static final List<String> GEOMDAN_KEYWORDS = List.of(
            "불로동", "원당", "당하동", "검단로", "왕길동", "마전동",
            "백석동", "오류동", "대곡동", "금곡동", "시천동", "완정로"
    );

    /** 인천 옛 중구 중 영종구로 편입된 지역의 주소 키워드 */
    private static final List<String> YEONGJONG_KEYWORDS = List.of(
            "공항로", "공항동로", "흰바위로", "백운로", "용유로",
            "운서", "운남", "중산동", "운북동", "을왕동"
    );

    private final RegionRepository regionRepository;

    private Map<String, Region> sigunguIndex;   // 최초 1회 로딩 후 재사용
    private Map<String, Region> dongIndex;      // 최초 1회 로딩 후 재사용

    /**
     * 도로명주소에서 시도/시군구를 추출해 일치하는 Region을 찾는다.
     * 시도/시군구를 찾지 못하면 동 이름으로 역추적을 시도한다.
     *
     * @param roadAddress 도로명주소 문자열
     * @return 매칭된 Region, 실패 시 null
     */
    public Region match(String roadAddress) {
        if (roadAddress == null || roadAddress.isBlank()) {
            return null;
        }

        // 일부 주소에 NBSP(U+00A0)가 섞여 있어 일반 공백으로 치환한다.
        // \s 정규식은 NBSP를 공백으로 인식하지 않아 토큰 분리가 실패한다.
        String normalized = roadAddress.replace('\u00A0', ' ').trim();

        String[] tokens = normalized.split("\\s+");
        if (tokens.length < 2) {
            return null;
        }

        String sido = SIDO_ALIAS.get(tokens[0]);

        // 시도명이 없는 주소는 동 이름으로 역추적
        // (예: "인창동 679-8 일원(구리도매시장)")
        if (sido == null) {
            return findDongIndex().get(tokens[0]);
        }

        String sigungu = resolveSigungu(sido, tokens[1], normalized);

        return findSigunguIndex().get(sido + "|" + sigungu);
    }

    /** 시군구명을 현재 행정구역 표기로 보정 */
    private String resolveSigungu(String sido, String rawSigungu, String roadAddress) {
        String sigungu = SIGUNGU_ALIAS.getOrDefault(rawSigungu, rawSigungu);

        if ("인천광역시".equals(sido)) {
            sigungu = resolveIncheonDistrict(sigungu, roadAddress);
        }

        return sigungu;
    }

    /**
     * 인천 개편 전 구 명칭을 현재 명칭으로 변환.
     * 서구·중구는 두 개 구로 분리되었으므로 주소 키워드로 판별한다.
     */
    private String resolveIncheonDistrict(String sigungu, String roadAddress) {
        String simpleMapped = INCHEON_SIMPLE_MAPPING.get(sigungu);
        if (simpleMapped != null) {
            return simpleMapped;
        }

        if ("서구".equals(sigungu)) {
            return containsAny(roadAddress, GEOMDAN_KEYWORDS) ? "검단구" : "서해구";
        }

        if ("중구".equals(sigungu)) {
            return containsAny(roadAddress, YEONGJONG_KEYWORDS) ? "영종구" : "제물포구";
        }

        return sigungu;
    }

    /** 주소에 키워드 목록 중 하나라도 포함되어 있는지 확인 */
    private boolean containsAny(String address, List<String> keywords) {
        return keywords.stream().anyMatch(address::contains);
    }

    /** 시군구 단위 대표 Region 인덱스를 1회 생성해 캐싱 */
    private Map<String, Region> findSigunguIndex() {
        if (sigunguIndex == null) {
            sigunguIndex = regionRepository.findAll().stream()
                    .collect(Collectors.toMap(
                            r -> r.getSido() + "|" + r.getSigungu(),
                            Function.identity(),
                            (a, b) -> a
                    ));
        }
        return sigunguIndex;
    }

    /**
     * 동 이름 → Region 인덱스를 1회 생성해 캐싱.
     * 시도/시군구가 없는 주소를 역추적할 때 사용한다.
     */
    private Map<String, Region> findDongIndex() {
        if (dongIndex == null) {
            dongIndex = regionRepository.findAll().stream()
                    .filter(r -> r.getDong() != null && !r.getDong().isBlank())
                    .collect(Collectors.toMap(
                            Region::getDong,
                            Function.identity(),
                            (a, b) -> a
                    ));
        }
        return dongIndex;
    }
}
