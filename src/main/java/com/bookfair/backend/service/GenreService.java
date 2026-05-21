package com.bookfair.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.bookfair.backend.dto.request.GenreRequest;
import com.bookfair.backend.dto.response.GenreResponse;
import com.bookfair.backend.model.Genre;
import com.bookfair.backend.repository.GenreRepository;

@Service
public class GenreService {

    private final GenreRepository genreRepository;

    public GenreService(GenreRepository genreRepository) {
        this.genreRepository = genreRepository;
    }

    public List<GenreResponse> getAllGenres() {
        return genreRepository.findAll().stream()
                .map(genre -> {
                    GenreResponse response = new GenreResponse();
                    response.setId(genre.getId());
                    response.setName(genre.getName());
                    response.setColorCode(genre.getColor());
                    return response;
                })
                .toList();
    }

    public GenreResponse getGenreById(Long id) {
        Genre genre =  genreRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Genre not found"));
        
        GenreResponse response = new GenreResponse();

        response.setId(genre.getId());
        response.setName(genre.getName());
        response.setColorCode(genre.getColor());
        return response;
    }

    public GenreResponse createGenre(GenreRequest genreRequest) {
        Genre genre = new Genre();

        genre.setName(genreRequest.getName());
        genre.setColor(genreRequest.getColorCode());
        genreRepository.save(genre);

        GenreResponse response = new GenreResponse();

        response.setId(genre.getId());
        response.setName(genre.getName());
        response.setColorCode(genre.getColor());

        return response;
    }

    public GenreResponse updateGenre(Long id, GenreRequest genreRequest) {
        Genre genre = genreRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Genre not found"));
        
        genre.setName(genreRequest.getName());
        genre.setColor(genreRequest.getColorCode());
        genreRepository.save(genre);

        GenreResponse response = new GenreResponse();

        response.setId(genre.getId());
        response.setName(genre.getName());
        response.setColorCode(genre.getColor());

        return response;
    }

    public void deleteGenre(Long id) {
        genreRepository.deleteById(id);
    }

    
}
