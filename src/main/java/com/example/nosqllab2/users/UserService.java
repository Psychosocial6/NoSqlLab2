package com.example.nosqllab2.users;

import com.example.nosqllab2.exceptions.UserAlreadyExistsException;
import com.example.nosqllab2.exceptions.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void deleteUserById(String id) {
        userRepository.deleteById(id).block();
    }

    public UserResponse createUser(UserRequest userRequest) {
        Boolean exists = userRepository.existsByNameIgnoreCase(userRequest.name()).block();
        if (Boolean.TRUE.equals(exists)) {
            throw new UserAlreadyExistsException("User already exists");
        }
        User user = new User(
                userRequest.name(),
                userRequest.email(),
                passwordEncoder.encode(userRequest.password()),
                "ПРЕПОДАВАТЕЛЬ"
        );
        User saved = userRepository.save(user).block();
        return UserResponse.fromEntity(saved);
    }

    public UserResponse getUserById(String id) {
        User user = userRepository.findById(id).block();
        if (user == null) {
            throw new UserNotFoundException("User not found");
        }
        return UserResponse.fromEntity(user);
    }

    public List<UserResponse> getUsers() {
        return userRepository.findAll()
                .collectList()
                .block()
                .stream()
                .map(UserResponse::fromEntity)
                .toList();
    }
}
