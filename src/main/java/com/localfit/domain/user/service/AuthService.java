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

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public Long signup(SignupRequest request){
        if(userRepository.existsByEmail(request.getEmail())){
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .role(UserRole.USER)
                .build();

        return userRepository.save(user).getId();
    }

    @Transactional(readOnly = true)
    public TokenResponse signIn(SignInRequest request){
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(()->new CustomException(ErrorCode.INVALID_CREDENTIALS));

        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())){
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        return issueTokens(user.getId(), user.getEmail());
    }

    @Transactional(readOnly = true)
    public TokenResponse refresh(RefreshRequest request){
        String refreshToken = request.getRefreshToken();

        if(!jwtTokenProvider.validateToken(refreshToken)){
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);

        if(!refreshTokenService.isValid(userId, refreshToken)){
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String email = jwtTokenProvider.getEmail(refreshToken);

        return issueTokens(userId, email);
    }

    public void signOut(Long userId){
        refreshTokenService.delete(userId);
    }

    private TokenResponse issueTokens(Long userId, String email){
        String accessToken = jwtTokenProvider.createAccessToken(userId, email);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId, email);

        refreshTokenService.save(userId, refreshToken);

        return new TokenResponse(accessToken,refreshToken);
    }
}
