package com.localfit.domain.subway.service;

import com.localfit.domain.region.entity.Region;
import com.localfit.domain.subway.dto.SubwayStationExcelRow;
import com.localfit.domain.subway.entity.SubwayStation;
import com.localfit.domain.subway.repository.SubwayStationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 리소스에 포함된 지하철역 표준데이터 엑셀 파일을 읽어 SubwayStation테이블에 동기화하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubwayStationSyncService {

    private final SubwayStationExcelReader excelReader;
    private final RegionAddressMatcher regionAddressMatcher;
    private final SubwayStationRepository subwayStationRepository;

    /**
     * 엑셀파일에서 지하철역 목록을 읽어 Region과 매칭해 저장
     *
     * @return 저장된 지하철역 건수
     */
    @Transactional
    public int sync() {
        List<SubwayStationExcelRow> rows = excelReader.readAll();

        int saved = 0;
        int skipped = 0;

        for (SubwayStationExcelRow row : rows) {
            if (subwayStationRepository.existsByStationCode(row.getStationCode())) {
                continue;
            }

            Region region = regionAddressMatcher.match(row.getRoadAddress());
            if (region == null) {
                skipped++;
                continue;
            }

            subwayStationRepository.save(SubwayStation.builder()
                    .stationName(row.getStationName())
                    .region(region)
                    .latitude(row.getLatitude())
                    .longitude(row.getLongitude())
                    .build());

            saved++;
        }
        log.info("[SubwayStationSync] 완료 - 저장 {}건, 매칭 실패로 제외 {}건", saved, skipped);
        return saved;
    }
}
