package com.example.nosqllab2.users;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

public record UserRequest(
        @NotEmpty(message = "name required")
        String name,
        @Pattern(
                regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$",
                message = "wrong email format"
        )
        String email,
        @NotEmpty(message = "password required")
        String password) {
}
