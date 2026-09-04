package com.localfit.domain.subway.service;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class SubwayStationNameNormalizer {

    private static final String SEOUL_STATION = "서울역";

    /** 괄호가 부기명이 아니라 역 구분자인 예외 케이스 */
    private static final Set<String> KEEP_PARENTHESES = Set.of("신촌(지하)");

    /**
     * 역명을 정규화
     * 괄호 부기명과 공백을 제거하고, 끝의 역 접미사를 제거한다.(서울역 제외)
     *
     * @param rawName 원본 역명
     * @return 정규화된 역명
     */
    public String normalize(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return "";
        }

        String name = rawName.replaceAll("\\s", "");

        // 괄호가 역 구분자인 경우 그대로 사용
        if (KEEP_PARENTHESES.contains(name)) {
            return name;
        }

        // 괄호와 그 안의 부기명 제거: "양재(서초구청)" → "양재"
        name = name.replaceAll("\\(.*?\\)", "");

        // 서울역은 '역'이 공식 역명의 일부이므로 접미사를 제거하지 않는다
        if (SEOUL_STATION.equals(name)) {
            return SEOUL_STATION;
        }

        // 끝의 '역' 접미사 제거: "창동역" → "창동"
        if (name.endsWith("역")) {
            name = name.substring(0, name.length() - 1);
        }

        return name;
    }
}
