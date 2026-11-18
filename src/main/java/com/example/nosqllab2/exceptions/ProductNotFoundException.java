package com.example.nosqllab2.exceptions;

import org.springframework.http.HttpStatus;

public class ProductNotFoundException extends ApplicationException {
    public ProductNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
