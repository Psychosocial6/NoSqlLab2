package com.example.nosqllab2.favorites;

import com.example.nosqllab2.products.ProductResponse;
import com.example.nosqllab2.users.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/favorite")
@RestController
public class FavoriteController {
    private final FavoriteService favorites;

    @GetMapping
    public List<ProductResponse> getFavorites(@AuthenticationPrincipal UserPrincipal principal) {
        return favorites.getUsersFavorites(principal.id());
    }

    @PostMapping("/{productId}")
    public ResponseEntity<Void> addFavorite(@AuthenticationPrincipal UserPrincipal principal, @PathVariable String productId) {
        boolean created = favorites.addFavorite(principal.id(), productId);
        return ResponseEntity.status(created ? HttpStatus.CREATED : HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFavorite(@AuthenticationPrincipal UserPrincipal principal, @PathVariable String productId) {
        favorites.deleteFavorite(principal.id(), productId);
    }
}
