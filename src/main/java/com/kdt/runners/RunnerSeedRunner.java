package com.kdt.runners;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RunnerSeedRunner implements ApplicationRunner {

    private final RunnerRepository runnerRepository;
    private final PasswordEncoder passwordEncoder;

    // 앱이 켜지면 한 번 자동 실행
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed("leader", "Leader1234!", "크루장", RunnerRole.LEADER);
        seed("member", "Member1234!", "러너", RunnerRole.MEMBER);
    }

    private void seed(String loginId, String rawPassword, String nickname, RunnerRole role) {
        // 이미 있으면 건너뜀 (재시작해도 중복 안 생김)
        if (runnerRepository.findByLoginId(loginId).isPresent()) {
            return;
        }
        Runner runner = new Runner();
        runner.setLoginId(loginId);
        runner.setPasswordHash(passwordEncoder.encode(rawPassword));
        runner.setNickname(nickname);
        runner.setRole(role);
        runnerRepository.save(runner);
    }
}