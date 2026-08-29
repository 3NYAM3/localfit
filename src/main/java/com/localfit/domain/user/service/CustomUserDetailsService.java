package com.localfit.domain.user.service;

import com.localfit.domain.user.repository.UserRepository;
import com.localfit.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security 인증 시 사용자 정보를 DB에서 조회하는 서비스
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * 이메일로 사용자를 조회해 UserDetails로 반환
     *
     * @param email 조회할 사용자 이메일
     * @return 인증에 사용할 UserDetails
     */
    @Override
    public UserDetails loadUserByUsername(String email) {
        return userRepository.findByEmail(email)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 회원입니다: " + email));
    }
}
