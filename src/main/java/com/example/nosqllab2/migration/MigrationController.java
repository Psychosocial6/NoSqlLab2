package com.example.nosqllab2.migration;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/migration")
@RestController
public class MigrationController {
    private final ProductMigrationService migrationService;

    @PostMapping("/status")
    public ProductMigrationService.MigrationResult runMigration() {
        return migrationService.migrateProductStatus();
    }
}
