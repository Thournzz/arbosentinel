package com.arbosentinel.white;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class DataIngestionException extends RuntimeException {

    public DataIngestionException(String message) {
        super(message);
    }

    public DataIngestionException(String message, Throwable cause) {
        super(message, cause);
    }
}
