package com.bookfair.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookfair.backend.model.Venue;

@Repository
public interface VenueRepository extends JpaRepository<Venue, UUID> {

    @EntityGraph(attributePaths = {
        "buildings",
        "buildings.floors",
        "buildings.floors.halls",
        "buildings.floors.halls.stalls"
    })
    Optional<Venue> findDetailedById(UUID id);

    Optional<Venue> findByNameAndActiveTrue(String name);

    boolean existsByNameAndActiveTrue(String name);
}