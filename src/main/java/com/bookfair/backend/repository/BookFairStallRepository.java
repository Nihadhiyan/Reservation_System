package com.bookfair.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bookfair.backend.model.BookFair;
import com.bookfair.backend.model.BookFairStall;
import com.bookfair.backend.model.BookFairStall.AvailabilityStatus;

@Repository
public interface BookFairStallRepository extends JpaRepository<BookFairStall, UUID> {

    List<BookFairStall> findByBookFairId(UUID bookFairId);

    List<BookFairStall> findByBookFair(BookFair bookFair);

    @Query("SELECT bfs FROM BookFairStall bfs JOIN FETCH bfs.stall WHERE bfs.bookfair.id = :bookFairId")
    List<BookFairStall> findAllByBookFairIdWithStallData(@Param("bookFairId") UUID bookFairId);

    List<BookFairStall> findByStatus(AvailabilityStatus status);

    Optional<BookFairStall> findByBookFairIdAndStallId(
        UUID bookFairId,
        UUID stallId
    );
}