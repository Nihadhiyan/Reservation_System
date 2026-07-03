package com.bookfair.backend.dto.genre.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.bookfair.backend.dto.config.GlobalMapperConfig;
import com.bookfair.backend.dto.genre.request.CreateGenreRequest;
import com.bookfair.backend.dto.genre.request.UpdateGenreRequest;
import com.bookfair.backend.dto.genre.response.GenreResponse;
import com.bookfair.backend.model.Genre;

@Mapper(config = GlobalMapperConfig.class)
public interface GenreMapper {
    
    GenreResponse toGenreResponse(Genre genre);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    @org.mapstruct.Mapping(target = "active", constant = "true")
    Genre toGenre(CreateGenreRequest request);

    void updateGenreFromRequest(UpdateGenreRequest request, @MappingTarget Genre genre);
}
