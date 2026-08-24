package com.localfit.domain.subway.service;

import com.localfit.domain.subway.config.SubwayProperties;
import com.localfit.domain.subway.dto.SubwayStationExcelRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubwayStationExcelReader {

    private static final int COL_STATION_CODE = 0;
    private static final int COL_STATION_NAME = 1;
    private static final int COL_LATITUDE = 9;
    private static final int COL_LONGITUDE = 10;
    private static final int COL_ROAD_ADDRESS = 12;

    private final SubwayProperties subwayProperties;

    public List<SubwayStationExcelRow> readAll() {
        List<SubwayStationExcelRow> result = new ArrayList<>();

        try (InputStream is = new ClassPathResource(subwayProperties.getDataFilePath()).getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;

                String stationCode = formatter.formatCellValue(row.getCell(COL_STATION_CODE)).trim();
                if (stationCode.isEmpty()) continue;

                String stationName = formatter.formatCellValue(row.getCell(COL_STATION_NAME)).trim();
                String roadAddress = formatter.formatCellValue(row.getCell(COL_ROAD_ADDRESS)).trim();
                Double latitude = parseDouble(row.getCell(COL_LATITUDE));
                Double longitude = parseDouble(row.getCell(COL_LONGITUDE));

                result.add(new SubwayStationExcelRow(stationCode, stationName, latitude, longitude, roadAddress));
            }

        } catch (Exception e) {
            log.error("[SubwayStationExcelReader] 엑셀 읽기 실패", e);
            throw new RuntimeException("지하철역 엑셀 파일을 읽는 중 오류가 발생했습니다.", e);
        }

        log.info("[SubwayStationExcelReader] 총 {}건 읽음", result.size());
        return result;
    }

    private Double parseDouble(Cell cell) {
        if (cell == null) return null;
        try {
            return cell.getNumericCellValue();
        } catch (Exception e){
            return null;
        }
    }
}
