package com.example.nosqllab2.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank @Size(max = 128) String name,
        @NotBlank @Email @Size(max = 254) String email,
        @NotEmpty @Size(max = 1024) String password
) {
}
