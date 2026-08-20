package com.localfit.domain.user.service;

import com.localfit.domain.region.entity.Region;
import com.localfit.domain.region.repository.RegionRepository;
import com.localfit.domain.user.dto.FavoriteRegionResponse;
import com.localfit.domain.user.entity.FavoriteRegion;
import com.localfit.domain.user.entity.User;
import com.localfit.domain.user.repository.FavoriteRegionRepository;
import com.localfit.domain.user.repository.UserRepository;
import com.localfit.global.exception.CustomException;
import com.localfit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteRegionService {

    private final FavoriteRegionRepository favoriteRegionRepository;
    private final UserRepository userRepository;
    private final RegionRepository regionRepository;

    @Transactional
    public Long add(Long userId, Long regionId) {
        if(favoriteRegionRepository.existsByUserIdAndRegionId(userId, regionId)){
            throw new CustomException(ErrorCode.ALREADY_FAVORITED);
        }

        User user = userRepository.getReferenceById(userId);
        Region region = regionRepository.findById(regionId).orElseThrow(()-> new CustomException(ErrorCode.REGION_NOT_FOUND));

        FavoriteRegion favoriteRegion = FavoriteRegion.builder()
                .user(user)
                .region(region)
                .build();

        return favoriteRegionRepository.save(favoriteRegion).getId();
    }

    @Transactional(readOnly = true)
    public List<FavoriteRegionResponse> getMyFavorites(Long userId){
        return favoriteRegionRepository.findAllByUserId(userId).stream()
                .map(FavoriteRegionResponse::new)
                .toList();
    }

    @Transactional
    public void remove(Long userId, Long regionId){
        FavoriteRegion favoriteRegion = favoriteRegionRepository.findByUserIdAndRegionId(userId, regionId)
                .orElseThrow(()-> new CustomException(ErrorCode.FAVORITE_NOT_FOUND));

        favoriteRegionRepository.delete(favoriteRegion);
    }
}
