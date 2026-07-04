package com.bookfair.backend.dto.genre.response;

import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GenreResponse implements Serializable {
    private UUID id;
    private String name;
    private Boolean active;
    private String color;
}
