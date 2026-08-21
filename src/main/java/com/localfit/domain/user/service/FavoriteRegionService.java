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

@Service
@RequiredArgsConstructor
public class FavoriteRegionService {

    private final FavoriteRegionRepository favoriteRegionRepository;
    private final UserRepository userRepository;
    private final RegionRepository regionRepository;
    private final CacheManager cacheManager;

    /**
     * 관심지역 목록을 한 번에 교체 등록한다.
     * 기존 관심지역을 모두 지우고 요청받은 목록으로 새로 저장한다.
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

        List<FavoriteRegion> newFavorites = requests.stream()
                .map(req -> FavoriteRegion.builder()
                        .user(user)
                        .region(regionMap.get(req.getRegionId()))
                        .priority(req.getPriority())
                        .build())
                .toList();

        favoriteRegionRepository.saveAll(newFavorites);

        evictFavoriteScoresCache(userId);
    }

    @Transactional(readOnly = true)
    public List<FavoriteRegionResponse> getMyFavorites(Long userId) {
        return favoriteRegionRepository.findAllByUserId(userId).stream()
                .map(FavoriteRegionResponse::new)
                .toList();
    }

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

        evictFavoriteScoresCache(userId);
    }

    private void evictFavoriteScoresCache(Long userId) {
        Cache cache = cacheManager.getCache(FavoriteRegionScoreService.CACHE_NAME);
        if (cache != null) {
            cache.evictIfPresent(userId + "::JEONSE");
            cache.evictIfPresent(userId + "::MONTHLY");
        }
    }
}
