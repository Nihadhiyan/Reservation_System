package com.bookfair.backend.exception;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.bookfair.backend.dto.common.ErrorResponse;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(BaseException.class)
        public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex) {

                if(ex.getStatus().is5xxServerError()) {
                        log.error("Server Error [{}]: {}", ex.getErrorCode(), ex.getMessage(), ex);
                } else {
                        log.warn("Business Exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
                }

                return ResponseEntity
                        .status(ex.getStatus())
                        .body(ErrorResponse.build(ex.getStatus(), ex.getMessage(), null, ex.getErrorCode()));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {

                List<String> errors = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                                .toList();

                log.warn("Validation failed: {}", errors);

                return ResponseEntity.badRequest()
                                .body(ErrorResponse.build(HttpStatus.BAD_REQUEST, "Validation failed", errors, ErrorCode.VALIDATION_ERROR));
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {

                List<String> errors = ex.getConstraintViolations()
                                .stream()
                                .map(ConstraintViolation::getMessage)
                                .toList();

                log.warn("Constraint validation failed: {}", errors);

                return ResponseEntity.badRequest()
                                .body(ErrorResponse.build(HttpStatus.BAD_REQUEST, "Validation failed", errors, ErrorCode.VALIDATION_ERROR));
        }

        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {

                log.warn("Authentication failed: {}", ex.getMessage());

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(ErrorResponse.build(HttpStatus.UNAUTHORIZED, "Authentication failed", null, ErrorCode.UNAUTHORIZED));
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {

                log.warn("Access denied: {}", ex.getMessage());

                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(ErrorResponse.build(HttpStatus.FORBIDDEN, "Access denied", null, ErrorCode.FORBIDDEN));
        }

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {

                log.error("Database constraint violation: {}", ex.getMessage(), ex);

                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(ErrorResponse.build(HttpStatus.CONFLICT, "Database constraint violation", null, ErrorCode.DATABASE_ERROR));
        }

        public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {

                log.warn("Illegal state encountered: {}", ex.getMessage());

                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(ErrorResponse.build(HttpStatus.CONFLICT, ex.getMessage(), null, ErrorCode.BUSINESS_RULE_VIOLATION));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {

                log.error("CRITICAL: An unexpected error occurred: {}", ex.getMessage(), ex);

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ErrorResponse.build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", null, ErrorCode.INTERNAL_SERVER_ERROR));
        }
}