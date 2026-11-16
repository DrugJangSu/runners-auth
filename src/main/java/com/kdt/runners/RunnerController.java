package com.kdt.runners;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/runners")
@RequiredArgsConstructor
public class RunnerController {

    private final RunnerService runnerService;

    @GetMapping
    List<RunnerResponse> list() {
        return runnerService.list();
    }

    @GetMapping("/{id}")
    RunnerResponse get(@PathVariable Long id) {
        return runnerService.get(id);
    }

    @PatchMapping("/{id}/role")                                        // ← 추가
    RunnerResponse changeRole(@PathVariable Long id,
                              @Valid @RequestBody RoleUpdateRequest request) {
        return runnerService.changeRole(id, request);
    }

    @DeleteMapping("/{id}")                                            // ← 추가
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        runnerService.delete(id);
    }
}