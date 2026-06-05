package com.bookfair.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookfair.backend.model.Stall;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StallRepository extends JpaRepository<Stall, UUID> {

    List<Stall> findByHallIdAndActiveTrue(UUID hallId);
        
    List<Stall> findByStallTypeAndActiveTrue(Stall.StallType stallType);

    List<Stall> findAllByActiveTrue();

    List<Stall> findAllByIdAndActiveTrue(List<UUID> stallIds);
        
    Optional<Stall> findByIdAndActiveTrue(UUID id);

    boolean existsByBookFairIdAndStallId(
        UUID bookFairId,
        UUID stallId
    );
}
