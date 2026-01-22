package com.mintstack.finance.repository;

import com.mintstack.finance.entity.SimulationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SimulationConfigRepository extends JpaRepository<SimulationConfig, UUID> {
    
    Optional<SimulationConfig> findFirstByOrderByCreatedAtDesc();
    
    default SimulationConfig getOrCreateDefault() {
        return findFirstByOrderByCreatedAtDesc()
                .orElseGet(() -> save(SimulationConfig.builder().build()));
    }
}
