package com.bookfair.backend.dto.common;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;

import com.bookfair.backend.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String errorMessage;
    private ErrorCode code;
    private Object details;
    
    public static ErrorResponse build(HttpStatus status, String errorMessage, Object details, ErrorCode code) {
        ErrorResponse response = new ErrorResponse();
        response.timestamp = LocalDateTime.now();
        response.status = status.value();
        response.errorMessage = errorMessage;
        response.details = details;
        response.code = code;

        return response;
    }
}
