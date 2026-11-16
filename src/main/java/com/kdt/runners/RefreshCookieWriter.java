package com.kdt.runners;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefreshCookieWriter {

    public static final String REFRESH = "refreshToken";

    private final long refreshSeconds;

    public RefreshCookieWriter(
            @Value("${kdt.jwt.refresh-seconds}") long refreshSeconds) {
        this.refreshSeconds = refreshSeconds;
    }

    // refresh 토큰을 HttpOnly 쿠키로 응답에 붙임 (로그인, 재발급 때)
    public void write(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH, token)
                .httpOnly(true)
                .secure(false)
                .path("/api/v1/auth")
                .sameSite("Lax")
                .maxAge(Duration.ofSeconds(refreshSeconds))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // 같은 이름·Path로 0초짜리 쿠키를 보내서 삭제 (로그아웃 때)
    public void clear(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH, "")
                .httpOnly(true)
                .secure(false)
                .path("/api/v1/auth")
                .sameSite("Lax")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}