package com.mintstack.finance.repository;

import com.mintstack.finance.entity.UserDataPreference;
import com.mintstack.finance.entity.UserDataPreference.DataType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDataPreferenceRepository extends JpaRepository<UserDataPreference, UUID> {
    
    List<UserDataPreference> findByUserId(UUID userId);
    
    Optional<UserDataPreference> findByUserIdAndDataType(UUID userId, DataType dataType);
    
    List<UserDataPreference> findByUserIdAndIsEnabledTrue(UUID userId);
    
    void deleteByUserId(UUID userId);
}
