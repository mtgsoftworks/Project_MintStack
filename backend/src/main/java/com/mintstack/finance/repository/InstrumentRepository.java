package com.mintstack.finance.repository;

import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.Instrument.InstrumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstrumentRepository extends JpaRepository<Instrument, UUID> {

    Optional<Instrument> findBySymbol(String symbol);

    List<Instrument> findByType(InstrumentType type);

    Page<Instrument> findByType(InstrumentType type, Pageable pageable);

    List<Instrument> findByTypeAndIsActiveTrue(InstrumentType type);

    Page<Instrument> findByTypeAndIsActiveTrue(InstrumentType type, Pageable pageable);

    List<Instrument> findByIsActiveTrue();

    @Query("SELECT i FROM Instrument i WHERE i.isActive = true AND " +
           "(LOWER(i.symbol) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Instrument> searchBySymbolOrName(@Param("query") String query, Pageable pageable);

    @Query("SELECT i FROM Instrument i WHERE i.type = :type AND i.isActive = true AND " +
           "(LOWER(i.symbol) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Instrument> searchByTypeAndQuery(@Param("type") InstrumentType type, 
                                          @Param("query") String query, 
                                          Pageable pageable);

    List<Instrument> findBySymbolIn(List<String> symbols);

    boolean existsBySymbol(String symbol);
}
