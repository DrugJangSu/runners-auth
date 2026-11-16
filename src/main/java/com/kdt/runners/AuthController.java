package com.kdt.runners;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final RefreshCookieWriter cookieWriter;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    RunnerResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        TokenPair tokens = authService.login(request);
        cookieWriter.write(response, tokens.refreshToken());
        return new LoginResponse(tokens.accessToken(), "Bearer", tokens.accessExpiresIn());
    }

    // 재발급: 쿠키의 refresh로 새 출입증 2장                          ← 추가
    @PostMapping("/refresh")
    LoginResponse refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {
        TokenPair tokens = authService.refresh(refreshToken);
        cookieWriter.write(response, tokens.refreshToken());     // 새 refresh로 쿠키 교체
        return new LoginResponse(tokens.accessToken(), "Bearer", tokens.accessExpiresIn());
    }

    // 로그아웃: refresh 쿠키 지우라고 지시                             ← 추가
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(HttpServletResponse response) {
        cookieWriter.clear(response);
    }

    // 내 정보 조회 (출입증 필요 → 경비원 통과해야 옴)
    @GetMapping("/me")
    RunnerResponse me() {
        return authService.me();
    }

    // 닉네임 수정
    @PatchMapping("/me")
    RunnerResponse updateMe(@Valid @RequestBody NicknameUpdateRequest request) {
        return authService.updateMe(request);
    }
}