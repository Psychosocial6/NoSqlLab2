package com.example.nosqllab2.users;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
public class User {
    @DocumentId
    private String id;
    private String name;
    private String email;
    private String passwordHash;
    // Present only on documents created before password hashing.
    private String password;
    private String role;

    public User(String name, String email, String passwordHash, String role) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }
}
