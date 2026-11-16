package com.kdt.runners;

public record RunnerResponse(
        Long id,
        String loginId,
        String nickname,
        RunnerRole role
) {
    public static RunnerResponse from (Runner runner) {
        return new RunnerResponse(
                runner.getId(),
                runner.getLoginId(),
                runner.getNickname(),
                runner.getRole()
        );
    }
}
