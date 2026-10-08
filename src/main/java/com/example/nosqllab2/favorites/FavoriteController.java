package com.example.nosqllab2.favorites;

import com.example.nosqllab2.products.ProductResponse;
import com.example.nosqllab2.users.User;
import com.example.nosqllab2.exceptions.UserNotFoundException;
import com.example.nosqllab2.users.UserRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RequiredArgsConstructor
@RequestMapping("/api/favorite")
@RestController
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getFavorites() {
        return ResponseEntity.ok(favoriteService.getUsersFavorites(getUserId()));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<Void> addFavorite(
            @PathVariable
            @NotNull(message = "id required")
            String productId) {
        favoriteService.addFavorite(getUserId(), productId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteFavorite(
            @PathVariable
            @NotNull(message = "id required")
            String productId) {
        favoriteService.deleteFavorite(getUserId(), productId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private String getUserId() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByNameIgnoreCase(currentUsername).block();
        if (user == null) {
            throw new UserNotFoundException("User not found");
        }
        return user.getId();
    }
}