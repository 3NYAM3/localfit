package com.localfit.domain.user.dto;

import com.localfit.domain.user.entity.User;
import lombok.Getter;

/**
 * 회원정보 조회 응답
 */
@Getter
public class UserResponse {

    private final Long id;
    private final String email;
    private final String nickname;

    public UserResponse(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.nickname = user.getNickname();
    }
}
