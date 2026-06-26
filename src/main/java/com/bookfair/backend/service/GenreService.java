package com.bookfair.backend.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookfair.backend.dto.genre.mapper.GenreMapper;
import com.bookfair.backend.dto.genre.request.CreateGenreRequest;
import com.bookfair.backend.dto.genre.response.GenreResponse;
import com.bookfair.backend.exception.BusinessException;
import com.bookfair.backend.exception.ErrorCode;
import com.bookfair.backend.exception.ResourceNotFoundException;
import com.bookfair.backend.model.Genre;
import com.bookfair.backend.repository.GenreRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GenreService {

    private final GenreRepository genreRepository;
    private final GenreMapper genreMapper;

    @Transactional(readOnly = true)
    public List<GenreResponse> getAllGenres() {
        return genreRepository.findByActiveTrue().stream()
                .map(genreMapper::toGenreResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public GenreResponse createGenre(CreateGenreRequest request) {
        if (genreRepository.existsByName(request.getName())) {
            throw new BusinessException("Genre name already exists", ErrorCode.BUSINESS_RULE_VIOLATION);
        }

        Genre genre = new Genre();
        genre.setName(request.getName());
        genre.setActive(true);

        Genre saved = genreRepository.save(genre);
        log.info("Created new genre: {}", saved.getName());
        
        return genreMapper.toGenreResponse(saved);
    }

    @Transactional
    public void deleteGenre(UUID genreId) {
        Genre genre = genreRepository.findByIdAndActiveTrue(genreId)
                .orElseThrow(() -> new ResourceNotFoundException("Genre not found", ErrorCode.BUSINESS_RULE_VIOLATION));

        genre.setActive(false);
        genreRepository.save(genre);
        log.info("Soft deleted genre: {}", genreId);
    }
}
