package com.localfit.domain.user.repository;

import com.localfit.domain.user.entity.FavoriteRegion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoriteRegionRepository extends JpaRepository<FavoriteRegion, Long> {

    /**
     * 사용자의 관심지역 목록을 우선순위 순으로 조회
     *
     * @param userId 사용자 ID
     * @return 관심지역 목록
     */
    @Query("""
            SELECT f FROM FavoriteRegion f
            JOIN FETCH f.region
            WHERE f.user.id = :userId
            ORDER BY f.priority ASC
            """)
    List<FavoriteRegion> findAllByUserId(@Param("userId") Long userId);

    /**
     * 특정 사용자의 관심지역을 전부 삭제한다.
     *
     * @param userId 삭제 대상 사용자 ID
     */
    @Modifying
    @Query("DELETE FROM FavoriteRegion f WHERE f.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
