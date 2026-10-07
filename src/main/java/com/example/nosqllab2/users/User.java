package com.example.nosqllab2.users;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
@Document(collectionName = "users")
public class User {
    @DocumentId
    private String id;
    private String name;
    private String email;
    private String password;
    private String role;

    public User(String name, String email, String password, String role) {
        this.id = String.valueOf(UUID.randomUUID());
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }
}