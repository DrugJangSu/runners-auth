package com.kdt.runners;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn

) {
}
