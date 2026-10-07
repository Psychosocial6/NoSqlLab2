package com.example.nosqllab2.operations;

import com.example.nosqllab2.models.OperationLog;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Validated
@RequiredArgsConstructor
@RequestMapping("/api/operations")
@RestController
public class OperationController {
    private final OperationService operationService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<OperationLog>> getOperations(@PathVariable @NotNull(message = "id required") @Positive(message = "id must be positive") Long userId) {
        return ResponseEntity.ok(operationService.getUserOperations(userId));
    }

    @GetMapping("/speedtest/{userId}")
    public ResponseEntity<Map<String, Object>> runBenchmark(
            @PathVariable @NotNull @Positive Long userId,
            @RequestParam(defaultValue = "50") int iterations) {
        return ResponseEntity.ok(operationService.speedtest(userId, iterations));
    }
}
