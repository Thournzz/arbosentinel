package com.arbosentinel.white;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class MlServiceException extends RuntimeException {

    public MlServiceException(String message) {
        super(message);
    }

    public MlServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
