package com.mintstack.finance.repository;

import com.mintstack.finance.entity.UserApiConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserApiConfigRepository extends JpaRepository<UserApiConfig, UUID> {
    List<UserApiConfig> findByUserId(UUID userId);
    Optional<UserApiConfig> findByUserIdAndProvider(UUID userId, UserApiConfig.ApiProvider provider);
    List<UserApiConfig> findByProviderAndIsActiveTrue(UserApiConfig.ApiProvider provider);
}
