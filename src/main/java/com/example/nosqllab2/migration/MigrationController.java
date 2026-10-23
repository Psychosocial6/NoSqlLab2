package com.example.nosqllab2.migration;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequiredArgsConstructor
@RequestMapping("/api/migration")
@RestController
public class MigrationController {

    private final ProductMigrationService migrationService;

    @PostMapping("/status")
    public ResponseEntity<Map<String, Object>> runMigration() {
        return ResponseEntity.ok(migrationService.migrateProductStatus());
    }
}