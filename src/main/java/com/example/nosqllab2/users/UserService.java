package com.example.nosqllab2.users;

import com.example.nosqllab2.exceptions.UserNotFoundException;
import com.example.nosqllab2.storage.DocumentPage;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionRegistry sessions;

    public UserResponse createUser(UserRequest request) {
        User user = new User(request.name().strip(), request.email().strip(),
                passwordEncoder.encode(request.password()), UserRoles.TEACHER);
        return UserResponse.fromEntity(userRepository.create(user));
    }

    public UserResponse getUserById(String id) {
        return userRepository.findById(id)
                .map(UserResponse::fromEntity)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    public DocumentPage<UserResponse> getUsers(int size, String cursor) {
        return userRepository.list(size, DocumentPage.decodeCursor(cursor));
    }

    public void deleteUserById(String id) {
        userRepository.delete(id);
        sessions.getAllPrincipals().stream()
                .filter(principal -> principal instanceof UserPrincipal user && id.equals(user.id()))
                .flatMap(principal -> sessions.getAllSessions(principal, false).stream())
                .forEach(SessionInformation::expireNow);
    }
}
