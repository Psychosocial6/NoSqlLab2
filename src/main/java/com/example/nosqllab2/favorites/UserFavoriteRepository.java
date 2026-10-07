package com.example.nosqllab2.favorites;

import com.example.nosqllab2.products.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserFavoriteRepository extends JpaRepository<UserFavoriteEntity, UserFavoriteId> {
    @Query("SELECT uf.product FROM UserFavoriteEntity uf WHERE uf.id.userId = :userId")
    List<ProductEntity> findProductsByUserId(@Param("userId") Long userId);
}