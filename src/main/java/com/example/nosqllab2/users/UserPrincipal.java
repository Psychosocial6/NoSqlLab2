package com.example.nosqllab2.users;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public final class UserPrincipal implements UserDetails, CredentialsContainer {
    private final String id;
    private final String name;
    private final String email;
    private final String role;
    private String password;

    UserPrincipal(User user, String password) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.password = password;
    }

    public String id() {
        return id;
    }

    public UserResponse response() {
        return new UserResponse(id, name, email, role);
    }

    @Override
    public String getUsername() {
        return name;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null || role.isBlank()) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public void eraseCredentials() {
        password = null;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof UserPrincipal principal && id.equals(principal.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
