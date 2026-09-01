package com.localfit.domain.hospital.repository;

import com.localfit.domain.hospital.dto.HospitalCountProjection;
import com.localfit.domain.hospital.entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    /** ykiho로 중복 여부 확인 */
    boolean existsByYkiho(String ykiho);

    /**
     * 수도권 전체 시군구별 병원 개수 집계
     *
     * @return 시군구별 병원 개수 목록
     */
    @Query("""
            SELECT h.region.sido AS sido, h.region.sigungu AS sigungu, COUNT(h) AS hospitalCount
            FROM Hospital h
            GROUP BY h.region.sido, h.region.sigungu
            """)
    List<HospitalCountProjection> countAllBySigungu();

    /**
     * 특정 시도 내 시군구별 병원 개수 집계
     *
     * @param sido 대상 시도명
     * @return 해당 시도 내 시군구별 병원 개수 목록
     */
    @Query("""
            SELECT h.region.sido AS sido, h.region.sigungu AS sigungu, COUNT(h) AS hospitalCount
            FROM Hospital h
            WHERE h.region.sido = :sido
            GROUP BY h.region.sido, h.region.sigungu
            """)
    List<HospitalCountProjection> countBySido(@Param("sido") String sido);
}
