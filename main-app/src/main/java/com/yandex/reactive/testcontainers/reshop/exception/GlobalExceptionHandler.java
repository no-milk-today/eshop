package com.yandex.reactive.testcontainers.reshop.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

// @ControllerAdvice will be replaced with routers
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public Mono<Rendering> handleResourceNotFound(ResourceNotFoundException ex) {
        return Mono.just(
                Rendering.view("not-found")
                        .modelAttribute("errorMessage", ex.getMessage())
                        .status(HttpStatus.NOT_FOUND)
                        .build()
        );
    }

    @ExceptionHandler({ IllegalArgumentException.class,
            RequestValidationException.class })
    public Mono<Rendering> handleIllegalArgumentException() {
        return Mono.just(
                Rendering.view("invalid-arguments")
                        .status(HttpStatus.BAD_REQUEST)
                        .build()
        );
    }

    @ExceptionHandler(Exception.class)
    public Mono<Rendering> handleGeneralException() {
        return Mono.just(
                Rendering.view("error")
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .build()
        );
    }
}