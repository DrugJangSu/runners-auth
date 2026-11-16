package com.kdt.runners;

public record TokenPair(
        String accessToken,
        String refreshToken,
        long accessExpiresIn
) {
}
