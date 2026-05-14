package com.mintstack.finance.repository;

import com.mintstack.finance.entity.RuntimeSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RuntimeSettingRepository extends JpaRepository<RuntimeSetting, UUID> {
    Optional<RuntimeSetting> findByKey(String key);
}
