package com.localfit.domain.user.controller;

import com.localfit.domain.user.dto.SignInRequest;
import com.localfit.domain.user.dto.SignupRequest;
import com.localfit.domain.user.dto.TokenResponse;
import com.localfit.domain.user.service.AuthService;
import com.localfit.domain.user.service.CustomUserDetailsService;
import com.localfit.global.exception.CustomException;
import com.localfit.global.exception.ErrorCode;
import com.localfit.global.security.jwt.JwtAuthenticationFilter;
import com.localfit.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController API 테스트
 */

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Nested
    @DisplayName("회원가입")
    class Signup {

        @Test
        @DisplayName("유효한 요청이면 200과 생성된 사용자 ID를 반환한다")
        void validRequest_returnsUserId() throws Exception {
            // given
            given(authService.signup(any())).willReturn(1L);

            // when & then
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    createSignupRequest("test@test.com", "Test1234!", "tester"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("회원가입 완료"))
                    .andExpect(jsonPath("$.data").value(1));
        }

        @Test
        @DisplayName("이메일 형식이 잘못되면 400을 반환한다")
        void invalidEmail_returns400() throws Exception {
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    createSignupRequest("not-an-email", "Test1234!", "tester"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("비밀번호가 8자 미만이면 400과 안내 메시지를 반환한다")
        void shortPassword_returns400() throws Exception {
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    createSignupRequest("test@test.com", "Ab1!", "tester"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("비밀번호는 8자 이상이어야 합니다."));
        }

        @Test
        @DisplayName("비밀번호에 특수문자가 없으면 400과 안내 메시지를 반환한다")
        void passwordWithoutSpecialChar_returns400() throws Exception {
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    createSignupRequest("test@test.com", "Test12345", "tester"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("비밀번호는 영문, 숫자, 특수문자를 각각 최소 1개 이상 포함해야 합니다."));
        }

        @Test
        @DisplayName("중복된 이메일이면 409를 반환한다")
        void duplicateEmail_returns409() throws Exception {
            // given
            willThrow(new CustomException(ErrorCode.DUPLICATE_EMAIL))
                    .given(authService).signup(any());

            // when & then
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    createSignupRequest("test@test.com", "Test1234!", "tester"))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."));
        }
    }

    @Nested
    @DisplayName("로그인")
    class SignIn {

        @Test
        @DisplayName("유효한 요청이면 Access/Refresh Token을 반환한다")
        void validRequest_returnsTokens() throws Exception {
            // given
            given(authService.signIn(any()))
                    .willReturn(new TokenResponse("access-token", "refresh-token"));

            // when & then
            mockMvc.perform(post("/api/auth/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    createSignInRequest("test@test.com", "Test1234!"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
        }

        @Test
        @DisplayName("비밀번호가 틀리면 401을 반환한다")
        void wrongPassword_returns401() throws Exception {
            // given
            willThrow(new CustomException(ErrorCode.INVALID_CREDENTIALS))
                    .given(authService).signIn(any());

            // when & then
            mockMvc.perform(post("/api/auth/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    createSignInRequest("test@test.com", "WrongPw123!"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다."));
        }
    }

    // ==================== 테스트 픽스처 ====================

    /** 회원가입 요청 생성. 필드가 private이라 리플렉션으로 주입한다 */
    private SignupRequest createSignupRequest(String email, String password, String nickname) {
        SignupRequest request = new SignupRequest();
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "password", password);
        ReflectionTestUtils.setField(request, "nickname", nickname);
        return request;
    }

    /** 로그인 요청 생성 */
    private SignInRequest createSignInRequest(String email, String password) {
        SignInRequest request = new SignInRequest();
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "password", password);
        return request;
    }
}
