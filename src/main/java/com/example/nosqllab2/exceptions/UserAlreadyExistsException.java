package com.example.nosqllab2.exceptions;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends ApplicationException {
    public UserAlreadyExistsException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
