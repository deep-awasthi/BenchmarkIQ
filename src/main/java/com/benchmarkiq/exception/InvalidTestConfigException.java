package com.benchmarkiq.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidTestConfigException extends RuntimeException {
    public InvalidTestConfigException(String message) {
        super(message);
    }
}
