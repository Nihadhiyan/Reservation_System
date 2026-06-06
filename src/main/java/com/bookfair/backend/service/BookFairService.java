package com.bookfair.backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.bookfair.backend.dto.bookfair.mapper.BookFairMapper;
import com.bookfair.backend.dto.bookfair.response.BookFairResponse;
import com.bookfair.backend.dto.bookfair.response.BookFairStallResponse;
import com.bookfair.backend.exception.ErrorCode;
import com.bookfair.backend.exception.ResourceNotFoundException;
import com.bookfair.backend.model.BookFair;
import com.bookfair.backend.model.BookFair.BookFairStatus;
import com.bookfair.backend.repository.BookFairRepository;
import com.bookfair.backend.repository.BookFairStallRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookFairService {
    private final BookFairRepository bookFairRepository;
    private final BookFairStallRepository bookFairStallRepository;
    private final BookFairMapper bookFairMapper;

    public List<BookFairResponse> getUpcomingBookFairs() {
        return bookFairRepository.findByStatusAndActiveTrue(BookFairStatus.UPCOMING).stream()
            .map(bookFair -> {
                return bookFairMapper.toBookFairResponse(bookFair);
            })
            .toList();
    }

    public List<BookFairStallResponse> getStallsForEvent(UUID bookFairId) {
        BookFair bookFair = bookFairRepository.findByIdAndActiveTrue(bookFairId)
            .orElseThrow(() -> new ResourceNotFoundException("Book fair event not found", ErrorCode.BOOKFAIR_NOT_FOUND));

        return bookFairStallRepository.findByBookFair(bookFair).stream().map(bookFairStall -> {
            return bookFairMapper.toBookFairStallResponse(bookFairStall);
        })
        .toList();
    }
}
