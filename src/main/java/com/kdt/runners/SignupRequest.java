package com.kdt.runners;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank
        @Pattern(regexp = "^[a-z0-9_]{4,20}$") String loginId,


        @NotBlank
        @Size(min = 8, max = 72)
        String password,


        @NotBlank
        @Size(min = 2, max = 20)
        String nickname
) {
}
