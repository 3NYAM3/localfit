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

/**
 * 관심지역 관리 API
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/favorites")
public class FavoriteRegionController {

    private final FavoriteRegionService favoriteRegionService;

    /**
     * 관심지역목록 교체
     *
     * @param userDetails 인증된 사용자 정보
     * @param request     등록할 관심지역 목록
     * @return 없음
     */
    @PutMapping
    public ApiResponse<Void> replaceAll(@AuthenticationPrincipal CustomUserDetails userDetails,
                                        @Valid @RequestBody FavoriteRegionBatchRequest request) {
        favoriteRegionService.replaceAll(userDetails.getUserId(), request.getFavorites());
        return ApiResponse.ok("관심지역 목록 저장 완료", null);
    }

    /**
     * 로그인한 사용자의 관심지역 목록을 우선순위 순으로 반환
     *
     * @param userDetails 인증된 사용자 정보
     * @return 관심지역 목록
     */
    @GetMapping
    public ApiResponse<List<FavoriteRegionResponse>> getMyFavorites(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(favoriteRegionService.getMyFavorites(userDetails.getUserId()));
    }

    /**
     * 특정관심지역 삭제
     *
     * @param userDetails 인증된 사용자 정보
     * @param regionId    삭제할 지역ID
     * @return 없음
     */
    @DeleteMapping("/{regionId}")
    public ApiResponse<Void> remove(@AuthenticationPrincipal CustomUserDetails userDetails,
                                    @PathVariable Long regionId) {
        favoriteRegionService.remove(userDetails.getUserId(), regionId);
        return ApiResponse.ok("삭제 완료", null);
    }
}
