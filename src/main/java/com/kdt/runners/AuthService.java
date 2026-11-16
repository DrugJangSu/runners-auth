package com.kdt.runners;

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
}
