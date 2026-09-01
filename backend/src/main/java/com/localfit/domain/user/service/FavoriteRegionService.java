package com.localfit.domain.user.service;

import com.localfit.domain.recommendation.service.FavoriteRegionScoreService;
import com.localfit.domain.region.entity.Region;
import com.localfit.domain.region.repository.RegionRepository;
import com.localfit.domain.user.dto.FavoriteRegionRequest;
import com.localfit.domain.user.dto.FavoriteRegionResponse;
import com.localfit.domain.user.entity.FavoriteRegion;
import com.localfit.domain.user.entity.User;
import com.localfit.domain.user.repository.FavoriteRegionRepository;
import com.localfit.domain.user.repository.UserRepository;
import com.localfit.global.exception.CustomException;
import com.localfit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 관심지역 관리 서비스
 */
@Service
@RequiredArgsConstructor
public class FavoriteRegionService {

    private final FavoriteRegionRepository favoriteRegionRepository;
    private final UserRepository userRepository;
    private final RegionRepository regionRepository;
    private final CacheManager cacheManager;

    /**
     * 관심지역 목록을 한 번에 교체 등록한다.
     *
     * @param userId   사용자 ID
     * @param requests 등록할 관신지역 목록
     */
    @Transactional
    public void replaceAll(Long userId, List<FavoriteRegionRequest> requests) {
        User user = userRepository.getReferenceById(userId);

        List<Long> regionIds = requests.stream().map(FavoriteRegionRequest::getRegionId).toList();
        List<Region> regions = regionRepository.findAllById(regionIds);

        if (regions.size() != regionIds.size()) {
            throw new CustomException(ErrorCode.REGION_NOT_FOUND);
        }

        Map<Long, Region> regionMap = regions.stream()
                .collect(Collectors.toMap(Region::getId, r -> r));

        favoriteRegionRepository.deleteAllByUserId(userId);

        favoriteRegionRepository.saveAll(requests.stream()
                .map(req -> FavoriteRegion.builder()
                        .user(user)
                        .region(regionMap.get(req.getRegionId()))
                        .priority(req.getPriority())
                        .build())
                .toList());
    }

    /**
     * 사용자의 관심지역 목록을 우선순위 순으로 반환
     *
     * @param userId 조회 대상 사용자 ID
     * @return 관심지역 목록
     */
    @Transactional(readOnly = true)
    public List<FavoriteRegionResponse> getMyFavorites(Long userId) {
        return favoriteRegionRepository.findAllByUserId(userId).stream()
                .map(FavoriteRegionResponse::new)
                .toList();
    }

    /**
     * 특정 관심지역을 삭제하고 나머지 항목의 우선순위를 재배열
     *
     * @param userId   사용자 ID
     * @param regionId 삭제할 지역 ID
     */
    @Transactional
    public void remove(Long userId, Long regionId) {
        List<FavoriteRegion> all = favoriteRegionRepository.findAllByUserId(userId);

        FavoriteRegion target = all.stream()
                .filter(f -> f.getRegion().getId().equals(regionId))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.FAVORITE_NOT_FOUND));

        int removedPriority = target.getPriority();

        // 삭제 대상보다 순위가 낮은(숫자가 큰) 항목들을 먼저 한 칸씩 당긴다
        all.stream()
                .filter(f -> f.getPriority() > removedPriority)
                .forEach(FavoriteRegion::decreasePriority);

        favoriteRegionRepository.delete(target);
    }
}
