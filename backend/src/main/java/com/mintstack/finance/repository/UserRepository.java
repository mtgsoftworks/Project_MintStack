package com.mintstack.finance.repository;

import com.mintstack.finance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByKeycloakId(String keycloakId);

    Optional<User> findByEmail(String email);

    @Query("SELECT u.isActive FROM User u WHERE u.keycloakId = :keycloakId")
    Optional<Boolean> findActiveStatusByKeycloakId(@Param("keycloakId") String keycloakId);

    boolean existsByKeycloakId(String keycloakId);

    boolean existsByEmail(String email);

    long countByIsActiveTrue();

    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%'))")
    org.springframework.data.domain.Page<User> searchByEmailOrName(@Param("query") String query, org.springframework.data.domain.Pageable pageable);
}
