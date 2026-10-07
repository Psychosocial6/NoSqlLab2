package com.example.nosqllab2.users;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByNameIgnoreCase(username).block();
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }


        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getName())
                .password(user.getPassword())
                .roles(user.getRole())
                .build();
    }
}