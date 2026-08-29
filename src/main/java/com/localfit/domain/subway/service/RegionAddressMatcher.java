package com.localfit.domain.subway.service;


import com.localfit.domain.region.entity.Region;
import com.localfit.domain.region.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 도로명주소 문자열에서 시도/시군구를 추출해 Region과 매칭하는 컴포넌트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegionAddressMatcher {

    /** 공공데이터 기관마다 다른 시도명 표기를 우리 Region 표기로 정규화하는 매핑 */
    private static final Map<String, String> SIDO_ALIAS = Map.of(
            "서울시", "서울특별시",
            "서울특별시", "서울특별시",
            "인천시", "인천광역시",
            "인천광역시", "인천광역시",
            "경기", "경기도",
            "경기도", "경기도"
    );

    private final RegionRepository regionRepository;

    private Map<String, Region> sigunguIndex;   //최초 1회 로딩 후 재사용

    /**
     * 도로명주소에서 시도/시군구를 추출해 일치하는 Region을 찾음
     *
     * @param roadAddress 도로명주소 문자열
     * @return 매칭된 Region
     */
    public Region match(String roadAddress) {
        if (roadAddress == null || roadAddress.isBlank()) {
            return null;
        }

        String[] tokens = roadAddress.trim().split("\\s+");
        if (tokens.length < 2) {
            return null;
        }

        String sido = SIDO_ALIAS.get(tokens[0]);
        if (sido == null) {
            return null;
        }

        String sigungu = tokens[1];

        return findSigunguIndex().get(sido + "|" + sigungu);
    }

    /** 시군구 단위 대표 Region 인덱스를 1회 생성해 캐싱한다 */
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
}
