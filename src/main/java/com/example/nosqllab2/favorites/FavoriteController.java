package com.example.nosqllab2.favorites;

import com.example.nosqllab2.products.ProductResponse;
import com.example.nosqllab2.users.UserEntity;
import com.example.nosqllab2.users.UserRepository;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
            @Positive(message = "id must be positive")
            Long productId) {
        favoriteService.addFavorite(getUserId(), productId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteFavorite(
            @PathVariable
            @NotNull(message = "id required")
            @Positive(message = "id must be positive")
            Long productId) {
        favoriteService.deleteFavorite(getUserId(), productId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private Long getUserId() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByNameIgnoreCase(currentUsername)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return user.getId();
    }
}