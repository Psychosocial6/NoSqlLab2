package com.example.nosqllab2.users;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void deleteUserById(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional
    public UserResponse createUser(UserRequest userRequest) {
        if (userRepository.existsByNameIgnoreCase(userRequest.name())) {
            throw new UserAlreadyExistsException("User already exists");
        }
        UserEntity user = new UserEntity(
                userRequest.name(),
                userRequest.email(),
                passwordEncoder.encode(userRequest.password()),
                "ПРЕПОДАВАТЕЛЬ"
        );
        return UserResponse.fromEntity(userRepository.save(user));
    }

    public UserResponse getUserById(Long id) {
        return userRepository.findById(id)
                .map(UserResponse::fromEntity)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    public List<UserResponse> getUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponse::fromEntity)
                .toList();
    }
}
