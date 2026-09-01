package com.localfit.domain.user.service;

import com.localfit.domain.user.dto.RefreshRequest;
import com.localfit.domain.user.dto.SignInRequest;
import com.localfit.domain.user.dto.SignupRequest;
import com.localfit.domain.user.dto.TokenResponse;
import com.localfit.domain.user.entity.User;
import com.localfit.domain.user.entity.UserRole;
import com.localfit.domain.user.repository.UserRepository;
import com.localfit.global.exception.CustomException;
import com.localfit.global.exception.ErrorCode;
import com.localfit.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 인증 관련 비즈니스 로직 서비스
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    /**
     * 신규회원 등록
     *
     * @param request 이메일, 비밀번호, 닉네임
     * @return 생성된 사용자 ID
     */
    @Transactional
    public Long signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        return userRepository.save(User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .role(UserRole.USER)
                .build()).getId();
    }

    /**
     * 로그인하여 Access/RefreshToken 발급
     *
     * @param request 이메일, 비밀번호
     * @return AccessToken, RefreshToken
     */
    @Transactional(readOnly = true)
    public TokenResponse signIn(SignInRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        return issueTokens(user.getId(), user.getEmail());
    }

    /**
     * RefreshToken을 검증해 새 Access/RefreshToken을 발급
     *
     * @param request RefreshToken
     * @return 새로 발급된 AccessToken, RefreshToken
     */
    @Transactional(readOnly = true)
    public TokenResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);

        if (!refreshTokenService.isValid(userId, refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        return issueTokens(userId, jwtTokenProvider.getEmail(refreshToken));
    }

    /**
     * 로그아웃
     *
     * @param userId 사용자ID
     */
    public void signOut(Long userId) {
        refreshTokenService.delete(userId);
    }

    /** AccessToken과 RefreshToken을 함께 발급하고 RefreshToken을 Redis에 저장 */
    private TokenResponse issueTokens(Long userId, String email) {
        String accessToken = jwtTokenProvider.createAccessToken(userId, email);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId, email);

        refreshTokenService.save(userId, refreshToken);

        return new TokenResponse(accessToken, refreshToken);
    }
}
