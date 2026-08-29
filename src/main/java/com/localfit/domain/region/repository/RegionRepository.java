package com.localfit.domain.region.repository;

import com.localfit.domain.region.entity.Region;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RegionRepository extends JpaRepository<Region, Long> {

    //특정 법정동코드 저장여부 확인
    boolean existsByRegionCode(String regionCode);

    /**
     * 시도/시군구/동 이름을 지역을 검색
     *
     * @param sido     시도필터
     * @param sigungu  시군구필터
     * @param keyword  동 이름 검색어
     * @param pageable 페이지 정보
     * @return 조건에 맞는 지역 목록
     */
    @Query("""
            SELECT r FROM Region r
            WHERE (:sido IS NULL OR r.sido = :sido)
              AND (:sigungu IS NULL OR r.sigungu = :sigungu)
              AND (:keyword IS NULL OR r.dong LIKE %:keyword%)
            """)
    Page<Region> search(
            @Param("sido") String sido,
            @Param("sigungu") String sigungu,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    /** 전월세 API호출 시 사용할 시군구 코드 목록 반환 */
    @Query("SELECT DISTINCT SUBSTRING(r.regionCode, 1, 5) FROM Region r")
    List<String> findDistinctSigunguCodes();

    /**
     * 시군구 코드(5자리)에 속한 모든 Region반환
     * 전월세 동기화 시 시군구 단위로 Region을 한 번에 캐싱해 건별 DB조회 방지
     */
    List<Region> findByRegionCodeStartingWith(String regionCodePrefix);
}
