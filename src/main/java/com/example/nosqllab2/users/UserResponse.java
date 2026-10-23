package com.example.nosqllab2.users;

public record UserResponse(String id, String name, String email, String role) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}