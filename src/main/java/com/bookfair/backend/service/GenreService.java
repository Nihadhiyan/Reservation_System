package com.bookfair.backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.bookfair.backend.dto.genre.mapper.GenreMapper;
import com.bookfair.backend.dto.genre.request.CreateGenreRequest;
import com.bookfair.backend.dto.genre.request.UpdateGenreRequest;
import com.bookfair.backend.dto.genre.response.GenreResponse;
import com.bookfair.backend.model.Genre;
import com.bookfair.backend.repository.GenreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GenreService {

    private final GenreRepository genreRepository;
    private final GenreMapper genreMapper;

    public List<GenreResponse> getAllGenres() {
        return genreRepository.findByActiveTrue().stream()
                .map(genre -> {
                    return genreMapper.toGenreResponse(genre);
                })
                .toList();
    }

    public GenreResponse getGenreById(UUID genreId) {
        Genre genre =  genreRepository.findByIdAndActiveTrue(genreId)
            .orElseThrow(() -> new IllegalArgumentException("Genre not found"));
        
        return genreMapper.toGenreResponse(genre);
    }

    public GenreResponse createGenre(CreateGenreRequest genreRequest) {

        Genre savedGenre = genreRepository.save(genreMapper.toGenre(genreRequest));

        return genreMapper.toGenreResponse(savedGenre);
    }

    public GenreResponse updateGenre(UUID genreId, UpdateGenreRequest genreRequest) {
        Genre genre = genreRepository.findByIdAndActiveTrue(genreId)
            .orElseThrow(() -> new IllegalArgumentException("Genre not found"));
        
        genreMapper.updateGenreFromRequest(genreRequest, genre);

        Genre updatedGenre = genreRepository.save(genre);   

        return genreMapper.toGenreResponse(updatedGenre);
    }

    public void deleteGenre(UUID genreId) {
        Genre genre = genreRepository.findByIdAndActiveTrue(genreId)
            .orElseThrow(() -> new IllegalArgumentException("Genre not found"));

        genre.setActive(false);

        genreRepository.save(genre);
    }

    
}
