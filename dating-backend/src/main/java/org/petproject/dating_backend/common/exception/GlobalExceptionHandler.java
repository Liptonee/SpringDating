package org.petproject.dating_backend.common.exception;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
@Hidden
public class GlobalExceptionHandler {

    @ExceptionHandler(PhotoException.class)
    public ResponseEntity<ErrorDto> handlePhoto(PhotoException e) {
        log.warn("Exception while processing photos.", e);
        return ResponseEntity.status(e.getStatusCode()).body(
                new ErrorDto(e.getStatusCode(),
                        "Exception while processing photos.",
                        e.getMessage())
        );
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ErrorDto> handleMinioException(ErrorResponseException e) {
        log.warn("Exception while processing photos.", e);
        return ResponseEntity.status(500).body(
                new ErrorDto(500,
                        "Storage error: " + e.errorResponse().code(),
                        e.getMessage())
        );
    }

    @ExceptionHandler(MinioException.class)
    public ResponseEntity<ErrorDto> handleMinioException(Exception e) {
        log.warn("Exception while processing photos.", e);
        return ResponseEntity.status(500).body(
                new ErrorDto(500,
                        "Storage unavailable",
                        e.getMessage())
        );
    }


    @ExceptionHandler(exception = MultipartException.class)
    public ResponseEntity<ErrorDto> handleMultipart(MultipartException e) {
        log.warn("Exception while processing photos.", e);
        return ResponseEntity.status(400).body(
                new ErrorDto(400,
                        "Exception while processing photos.",
                        e.getMessage())
        );
    }

    @ExceptionHandler({NotFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ErrorDto> handleNotFound(Exception e) {
        log.warn("Resource not found.", e);

        return ResponseEntity.status(404).body(
                new ErrorDto(404,
                        "Resource not found.",
                        e.getMessage())
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDto> handleNotValid(MethodArgumentNotValidException e) {
        log.warn("Validation failed.", e);
        Map<String, String> errors = new HashMap<>();

        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        return ResponseEntity.status(400).body(
                new ErrorDto(400,
                        "Validation failed.",
                        errors.toString())
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorDto> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("Controller doesn't supported this method",e);
        return ResponseEntity.status(400)
                .body(new ErrorDto(400,
                        "Controller doesn't supported this method",
                        e.getMessage() + "for this controller. Supported: " + e.getSupportedHttpMethods()));
    }



    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDto> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("Undefined Bad request",e);
        return ResponseEntity.status(400)
                .body(new ErrorDto(400,
                        "Bad request",
                        "Undefined Bad request"));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorDto> handleConflict(ConflictException e) {
        log.warn("Conflict data", e);
        return ResponseEntity.status(409)
                .body(new ErrorDto(409,
                        "Conflict data",
                        e.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorDto> handleUnauthorized(UnauthorizedException e) {
        log.warn("Unauthorized", e);
        return ResponseEntity.status(401)
                .body(new ErrorDto(401,
                        "Unauthorized",
                        e.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorDto> handleBadCredentials(BadCredentialsException e) {
        log.warn("Invalid login or password", e);
        return ResponseEntity.status(401)
                .body(new ErrorDto(401,
                        "Invalid login or password",
                        e.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorDto> handleForbidden(ForbiddenException e) {
        log.warn("User doesn't have permission", e);
        return ResponseEntity.status(403)
                .body(new ErrorDto(403,
                        "User doesn't have permission",
                        e.getMessage()));
    }

    @ExceptionHandler({ExpiredJwtException.class, MalformedJwtException.class})
    public ResponseEntity<ErrorDto> handleJwt(Exception e) {
        log.warn("JWT Token is invalid or expired", e);
        return ResponseEntity.status(401)
                .body(new ErrorDto(401,
                        "JWT Token is invalid or expired",
                        e.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorDto> handleAllBusiness(BusinessException e) {
        log.error("Some undefined business exception.", e);
        return ResponseEntity.status(e.getStatusCode()).body(
                new ErrorDto(e.getStatusCode(),
                        "Some undefined business exception.",
                        e.getMessage())
        );
    }

    @ExceptionHandler(InternalServerException.class)
    public ResponseEntity<ErrorDto> handleInternalServer(Exception e) {
        log.error("Internal Server Error.", e);
        return ResponseEntity.status(500).body(
                new ErrorDto(500,
                        "Internal Server Error.",
                        e.getMessage())
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleAll(Exception e) {
        log.error("Internal Server Error.", e);

        return ResponseEntity.status(500).body(
                new ErrorDto(500,
                        "Internal Server Error.",
                        "An unexpected error occurred")
        );
    }

}
