package com.benchmarkiq.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class TestAlreadyRunningException extends RuntimeException {
    public TestAlreadyRunningException(Long configId) {
        super("A test is already running for config id: " + configId);
    }
    public TestAlreadyRunningException(String message) {
        super(message);
    }
}
