package com.kdt.runners;

import jakarta.validation.constraints.NotNull;

public record RoleUpdateRequest(
        @NotNull RunnerRole role
) {
}