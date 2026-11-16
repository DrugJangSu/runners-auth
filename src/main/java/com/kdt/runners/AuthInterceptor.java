package com.kdt.runners;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtService jwtService;
    private final RunnerRepository runnerRepository;
    private final CurrentRunner currentRunner;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 컨트롤러 메서드로 가는 요청이 아니면 그냥 통과
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 2. 헤더에서 "Bearer " 떼고 출입증만 꺼내기 (없으면 401)
        String token = jwtService.bearerValue(request.getHeader("Authorization"));

        // 3. 출입용(access) 출입증 맞는지 검사 (아니면 401)
        Claims claims = jwtService.parseAccess(token);

        // 4. 주인 "1" → 숫자 1 (숫자 아니면 401)
        Long runnerId;
        try {
            runnerId = Long.parseLong(claims.getSubject());
        } catch (NumberFormatException ex) {
            throw new TokenFailedException();
        }

        // 5. DB에 그 사람 있나? 탈퇴했으면 401
        Runner runner = runnerRepository.findById(runnerId)
                .orElseThrow(TokenFailedException::new);

        // 6. 이번 요청의 상자에 "이 사람이 보냄" 저장
        currentRunner.setRunner(runner);

        // 7. 통과 → 컨트롤러로
        return true;
    }
}