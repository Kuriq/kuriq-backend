package com.example.kuriq.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {
    private final String error;
    private final HttpStatus status;

    public ApiException(String error, String message, HttpStatus status) {
        super(message);
        this.error = error;
        this.status = status;
    }
}
