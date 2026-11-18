package com.example.nosqllab2.operations;

import com.example.nosqllab2.users.UserPrincipal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RequiredArgsConstructor
@RequestMapping("/api/operations")
@RestController
public class OperationController {
    private final OperationService operations;

    @GetMapping("/{userId}")
    public ResponseEntity<List<OperationLogDTO>> getOperations(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable String userId,
            @RequestParam(defaultValue = "100") @Min(1) @Max(200) int size,
            @RequestParam(required = false) String after) {
        if (!principal.id().equals(userId)) {
            throw new AccessDeniedException("History belongs to another user");
        }
        return operations.getUserOperations(principal.id(), size, after).response();
    }

    @GetMapping("/speedtest/{userId}")
    public OperationService.Benchmark runBenchmark(@PathVariable String userId,
                                                    @RequestParam(defaultValue = "50") @Min(1) @Max(100) int iterations) {
        return operations.speedtest(userId, iterations);
    }
}
