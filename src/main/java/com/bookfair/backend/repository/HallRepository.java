package com.bookfair.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookfair.backend.model.Hall;

@Repository
public interface HallRepository extends JpaRepository<Hall, UUID> {

    List<Hall> findByFloorIdAndActiveTrue(UUID floorId);

    List<Hall> findByHallTypeAndActiveTrue(Hall.HallType hallType);

    List<Hall> findByActiveTrue();

    long countById(UUID Id);
}