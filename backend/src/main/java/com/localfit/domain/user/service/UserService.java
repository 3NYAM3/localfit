package com.localfit.domain.user.service;

import com.localfit.domain.user.dto.NicknameUpdateRequest;
import com.localfit.domain.user.dto.PasswordUpdateRequest;
import com.localfit.domain.user.dto.UserResponse;
import com.localfit.domain.user.entity.User;
import com.localfit.domain.user.repository.UserRepository;
import com.localfit.global.exception.CustomException;
import com.localfit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원정보 조회 및 변경 서비스
 */

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    /**
     * 회원 정보 조회
     *
     * @param userId 조회 대상 사용자 ID
     * @return 이메일, 닉네임 등 공개 가능한 정보
     */
    @Transactional(readOnly = true)
    public UserResponse getMyInfo(Long userId) {
        return new UserResponse(findUser(userId));
    }

    /**
     * 닉네임 변경
     *
     * @param userId  사용자 ID
     * @param request 새 닉네임
     * @return 변경된 회원 정보
     */
    @Transactional
    public UserResponse updateNickname(Long userId, NicknameUpdateRequest request) {
        User user = findUser(userId);
        user.changeNickname(request.getNickname());

        return new UserResponse(user);
    }

    @Transactional
    public void updatePassword(Long userId, PasswordUpdateRequest request) {
        User user = findUser(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }

        user.changePassword(passwordEncoder.encode(request.getNewPassword()));

        //비밀번호가 바뀌었으므로 기존RefreshToken폐기
        refreshTokenService.delete(userId);
    }

    /** 사용자 조회 */
    private User findUser(Long userId){
        return userRepository.findById(userId)
                .orElseThrow(()-> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
