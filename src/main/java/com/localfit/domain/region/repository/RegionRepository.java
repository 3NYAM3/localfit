package com.localfit.domain.region.repository;

import com.localfit.domain.region.entity.Region;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, Long> {

    //특정 법정동코드 저장여부 확인
    boolean existsByRegionCode(String regionCode);


    //시도,시군구,동 이름으로 부분검색+페이징
    //jpa반환형이 Page<T> 일 경우 개발자가 짠 조회쿼리 외에 같은 where 조건으로 count쿼리가 자동으로 실행된다
    //전체건수가 꼭 필요 없는 경우에는 다른 반환형(Slice<T>같은)을 사용하여 최적화 할 수 있다.   이 건은 페이지번호기반이라 제외
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

    //전월세 api호출용
    @Query("SELECT DISTINCT SUBSTRING(r.regionCode, 1, 5) FROM Region r")
    List<String> findDistinctSigunguCodes();

    // 시군구코드(5자리)에 속한 모든 Region 조회 - 동기화 시 메모리 캐싱용
    List<Region> findByRegionCodeStartingWith(String regionCodePrefix);

    //시군구코드+동 이름으로 region1 건 조회( 전월세 데이터를 region에 매칭할 때 사용)
    //Optional<Region> findByRegionCodeStartingWithAndDong(String regionCodePrefix, String dong);


}
