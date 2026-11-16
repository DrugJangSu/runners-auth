package com.kdt.runners;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final RunnerRepository runnerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentRunner currentRunner;

    @Transactional
    public RunnerResponse signup(SignupRequest request) {
        // 1. 비밀번호 바이트가 72 넘으면 → InvalidInputException
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidInputException();
        }
        // 2. 같은 loginId 있으면 → DuplicateLoginIdException
        if (runnerRepository.existsByLoginId(request.loginId())) {
            throw new DuplicateLoginIdException();
        }
        // 3. new Runner() 만들고 loginId, nickname, role=MEMBER 넣기
        Runner runner = new Runner();
        runner.setLoginId(request.loginId());
        runner.setNickname(request.nickname());
        runner.setRole(RunnerRole.MEMBER);

        // 4. passwordHash에 passwordEncoder.encode(비번) 넣기
        runner.setPasswordHash(passwordEncoder.encode(request.password()));
        // 5. save 하고 RunnerResponse.from(...)으로 돌려주기
        return RunnerResponse.from(runnerRepository.save(runner));
    }

    @Transactional(readOnly = true)
    public TokenPair login(LoginRequest request) {
        // 1. 비번 72바이트 넘으면 → 401 (가입 땐 400이었는데 로그인은 401)
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new LoginFailedException();
        }

        // 2. 아이디로 찾기. 없으면 → 401
        Runner runner = runnerRepository.findByLoginId(request.loginId())
                .orElseThrow(LoginFailedException::new);

        // 3. 비번 비교. 틀리면 → 401
        if (!passwordEncoder.matches(request.password(), runner.getPasswordHash())) {
            throw new LoginFailedException();
        }

        // 4. 출입증 2장 발급 (주인 = DB id)
        String access = jwtService.createAccess(runner.getId());
        String refresh = jwtService.createRefresh(runner.getId());

        // 5. 상자에 담아서 돌려줌
        return new TokenPair(access, refresh, jwtService.accessSeconds());
    }

    @Transactional(readOnly = true)
    public TokenPair refresh(String refreshToken) {
        // 1. 쿠키가 없거나 비었으면 → 401
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new TokenFailedException();
        }

        // 2. 재발급용 출입증 맞는지 검사 (직인·만료·용도)
        Claims claims = jwtService.parseRefresh(refreshToken);

        // 3. 출입증 주인 "1" → 숫자 1로 바꾸기. 숫자가 아니면 → 401
        Long runnerId;
        try {
            runnerId = Long.parseLong(claims.getSubject());
        } catch (NumberFormatException ex) {
            throw new TokenFailedException();
        }

        // 4. 그 사람이 DB에 아직 있나? 탈퇴했으면 → 401
        Runner runner = runnerRepository.findById(runnerId)
                .orElseThrow(TokenFailedException::new);

        // 5. 새 출입증 2장
        return new TokenPair(
                jwtService.createAccess(runner.getId()),
                jwtService.createRefresh(runner.getId()),
                jwtService.accessSeconds()
        );
    }

    // 내 정보 조회: 경비원이 상자에 넣어 둔 사람을 꺼내서 응답
    @Transactional(readOnly = true)
    public RunnerResponse me() {
        Runner runner = currentRunner.getRunner();
        if (runner == null) {
            throw new TokenFailedException();
        }
        return RunnerResponse.from(runner);
    }

    // 닉네임 수정
    @Transactional
    public RunnerResponse updateMe(NicknameUpdateRequest request) {
        Runner caller = currentRunner.getRunner();
        if (caller == null) {
            throw new TokenFailedException();
        }
        // 경비원이 꺼낸 Runner로 바로 고치지 말고, 여기서 다시 꺼내서 고침
        Runner runner = runnerRepository.findById(caller.getId())
                .orElseThrow(TokenFailedException::new);
        runner.setNickname(request.nickname());
        runnerRepository.save(runner);
        return RunnerResponse.from(runner);
    }
}
