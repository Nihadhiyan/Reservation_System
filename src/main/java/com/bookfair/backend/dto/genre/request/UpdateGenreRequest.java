package com.bookfair.backend.dto.genre.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGenreRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Active is required")
    private Boolean active;
    
    @NotBlank(message = "Color is required")
    private String color;







}
