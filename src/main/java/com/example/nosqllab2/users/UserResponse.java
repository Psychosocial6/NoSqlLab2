package com.example.nosqllab2.users;

public record UserResponse(Long id, String name, String email, String role) {
    public static UserResponse fromEntity(UserEntity userEntity) {
        return new UserResponse(
                userEntity.getId(),
                userEntity.getName(),
                userEntity.getEmail(),
                userEntity.getRole()
        );
    }
}