package com.example.gitpilot.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class AIUnavailableException extends RuntimeException {
    public AIUnavailableException(String message) {
        super(message);
    }
}
