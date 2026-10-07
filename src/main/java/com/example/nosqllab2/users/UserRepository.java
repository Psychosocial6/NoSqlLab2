package com.example.nosqllab2.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}