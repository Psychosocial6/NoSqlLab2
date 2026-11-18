package com.example.nosqllab2.users;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService, UserDetailsPasswordService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        String credential = PasswordHashes.credential(user);
        if (credential == null) {
            throw new UsernameNotFoundException("User has no credential");
        }
        return new UserPrincipal(user, credential);
    }

    @Override
    public UserDetails updatePassword(UserDetails user, String password) {
        UserPrincipal principal = (UserPrincipal) user;
        if (!userRepository.upgradePassword(principal.id(), user.getPassword(), password)) {
            throw new CredentialsExpiredException("Credential changed during sign-in");
        }
        return principal;
    }
}
