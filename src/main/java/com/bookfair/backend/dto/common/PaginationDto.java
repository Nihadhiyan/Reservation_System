package com.bookfair.backend.dto.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaginationDto {
    @NotNull(message = "Page is required")
    private Integer page;

    @NotNull(message = "Size is required")
    private Integer size;

    @NotNull(message = "Total elements is required")
    private Long totalElements;
    
    @NotNull(message = "Total pages is required")
    private Integer totalPages;
}
