package com.bookfair.backend.dto.bookfair.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookFairRequest {

    @NotBlank(message = "Book fair name is required")
    private String name;

    @NotBlank(message = "Book fair location is required")
    private String location;

    @NotBlank(message = "Book fair start date is required")
    private LocalDate startDate;

    @NotBlank(message = "Book fair end date is required")
    private LocalDate endDate;

    @NotBlank(message = "Book fair status is required")
    private String status;
}
