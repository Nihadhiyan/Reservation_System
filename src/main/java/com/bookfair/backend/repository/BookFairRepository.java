package com.bookfair.backend.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookfair.backend.model.BookFair;
import com.bookfair.backend.model.BookFair.BookFairStatus;

@Repository
public interface BookFairRepository extends JpaRepository<BookFair, UUID> {

    List<BookFair> findByStatusAndActiveTrue(BookFairStatus status);

    Optional<BookFair> findByIdAndActiveTrue(UUID id);

    List<BookFair> findByStartDateBeforeAndEndDateAfter(
        LocalDate currentDate1,
        LocalDate currentDate2
    );

    List<BookFair> findByStartDateBeforeAndEndDateAfterAndActiveTrue(
        LocalDate currentDate1,
        LocalDate currentDate2
    );
}