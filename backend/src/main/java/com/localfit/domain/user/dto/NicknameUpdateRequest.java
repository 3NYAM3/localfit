package com.localfit.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 닉네임 변경요청
 */

@Getter
@NoArgsConstructor
public class NicknameUpdateRequest {

    @NotBlank
    @Size(max = 30)
    private String nickname;
}
