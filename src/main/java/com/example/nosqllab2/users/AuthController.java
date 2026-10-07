package com.example.nosqllab2.users;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RequestMapping("/api/auth")
@RestController
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    public record LoginRequest(String username, String password) {}

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password())
        );

        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(authentication);
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

        User user = userRepository.findByNameIgnoreCase(loginRequest.username()).block();
        if (user == null) {
            throw new UserNotFoundException("User not found");
        }

        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        User user = userRepository.findByNameIgnoreCase(authentication.getName()).block();
        if (user == null) {
            throw new UserNotFoundException("User not found");
        }
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }
}