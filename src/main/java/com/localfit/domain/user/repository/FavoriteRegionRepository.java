package com.localfit.domain.user.repository;

import com.localfit.domain.user.entity.FavoriteRegion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoriteRegionRepository extends JpaRepository<FavoriteRegion, Long> {

    //특정 사용자의 관심지역 목록을 Region정보와 함께 조회
    @Query("""
            SELECT f FROM FavoriteRegion f
            JOIN FETCH f.region
            WHERE f.user.id = :userId
            ORDER BY f.createdAt DESC
            """)
    List<FavoriteRegion> findAllByUserId(@Param("userId")Long userId);

    Optional<FavoriteRegion> findByUserIdAndRegionId(Long userId, Long regionId);

    boolean existsByUserIdAndRegionId(Long userId, Long regionId);
}
