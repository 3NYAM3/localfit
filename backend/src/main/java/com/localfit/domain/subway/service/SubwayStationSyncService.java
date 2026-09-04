package com.localfit.domain.subway.service;

import com.localfit.domain.region.entity.Region;
import com.localfit.domain.subway.dto.SubwayStationExcelRow;
import com.localfit.domain.subway.entity.SubwayStation;
import com.localfit.domain.subway.repository.SubwayStationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 리소스에 포함된 지하철역 표준데이터 엑셀 파일을 읽어 SubwayStation테이블에 동기화하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubwayStationSyncService {

    private final SubwayStationExcelReader excelReader;
    private final RegionAddressMatcher regionAddressMatcher;
    private final SubwayStationNameNormalizer nameNormalizer;
    private final SubwayStationRepository subwayStationRepository;

    /**
     * 엑셀파일에서 지하철역 목록을 읽어 Region과 매칭해 저장
     *
     * @return 저장된 지하철역 건수
     */
    @Transactional
    public int sync() {
        List<SubwayStationExcelRow> rows = excelReader.readAll();

        //같은 배치 내 중복을 메모리에서 판별하기 위한 캐시
        Set<String> processedKeys = new HashSet<>();

        int saved = 0;
        int duplicated = 0;
        int skipped = 0;

        for (SubwayStationExcelRow row : rows) {
            Region region = regionAddressMatcher.match(row.getRoadAddress());
            if (region == null) {
                log.debug("[SubwayStationSync] 매칭 실패 - {} ({})", row.getStationName(), row.getRoadAddress());
                skipped++;
                continue;
            }

            String normalizedName = nameNormalizer.normalize(row.getStationName());
            String key = normalizedName + "|" + region.getSido() + "|" + region.getSigungu();

            // 같은 배치 내 중복 (환승역이 노선별로 여러 행에 존재)
            if (!processedKeys.add(key)) {
                duplicated++;
                continue;
            }

            // 이전 실행에서 이미 저장된 경우
            if (subwayStationRepository.existsByNormalizedNameAndRegionId(
                    normalizedName, region.getId())) {
                duplicated++;
                continue;
            }

            subwayStationRepository.save(SubwayStation.builder()
                    .stationName(row.getStationName())
                    .normalizedName(normalizedName)
                    .region(region)
                    .latitude(row.getLatitude())
                    .longitude(row.getLongitude())
                    .build());

            saved++;
        }

        log.info("[SubwayStationSync] 완료 - 저장 {}건, 중복 제외 {}건, 매칭 실패로 제외 {}건", saved, duplicated, skipped);
        return saved;
    }
}
