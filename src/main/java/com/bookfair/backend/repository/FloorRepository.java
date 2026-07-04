package com.bookfair.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookfair.backend.model.Floor;

@Repository
public interface FloorRepository extends JpaRepository<Floor, UUID> {
    List<Floor> findByBuildingId(UUID buildingId);
    List<Floor> findByBuildingIdOrderByLevelNumberAsc(UUID buildingId);
    List<Floor> findByBuildingIdAndActiveTrue(UUID buildingId);
}
