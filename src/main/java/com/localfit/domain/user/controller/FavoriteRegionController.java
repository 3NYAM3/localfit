package com.localfit.domain.user.controller;

import com.localfit.domain.user.dto.FavoriteRegionBatchRequest;
import com.localfit.domain.user.dto.FavoriteRegionRequest;
import com.localfit.domain.user.dto.FavoriteRegionResponse;
import com.localfit.domain.user.service.FavoriteRegionService;
import com.localfit.global.common.ApiResponse;
import com.localfit.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/favorites")
public class FavoriteRegionController {

    private final FavoriteRegionService favoriteRegionService;

    @PutMapping
    public ApiResponse<Void> replaceAll(@AuthenticationPrincipal CustomUserDetails userDetails,
                                        @Valid @RequestBody FavoriteRegionBatchRequest request) {
        favoriteRegionService.replaceAll(userDetails.getUserId(), request.getFavorites());
        return ApiResponse.ok("관심지역 목록 저장 완료", null);
    }

    @GetMapping
    public ApiResponse<List<FavoriteRegionResponse>> getMyFavorites(@AuthenticationPrincipal CustomUserDetails userDetails){
        return ApiResponse.ok(favoriteRegionService.getMyFavorites(userDetails.getUserId()));
    }

    @DeleteMapping("/{regionId}")
    public ApiResponse<Void> remove(@AuthenticationPrincipal CustomUserDetails userDetails,
                                    @PathVariable Long regionId){
        favoriteRegionService.remove(userDetails.getUserId(), regionId);
        return ApiResponse.ok("삭제완료",null);
    }
}
