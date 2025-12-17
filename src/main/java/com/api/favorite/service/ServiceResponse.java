package com.api.favorite.service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ServiceResponse<T> {
    private final HttpStatus status;
    private final T data;

    private ServiceResponse(HttpStatus status, T data) {
        this.status = status;
        this.data = data;
    }

    public static <T> ServiceResponse<T> ok(T data) {
        return new ServiceResponse<>(HttpStatus.OK, data);
    }

    public static <T> ServiceResponse<T> of(HttpStatus status, T data) {
        return new ServiceResponse<>(status, data);
    }

    public ResponseEntity<T> toResponseEntity() {
        return ResponseEntity.status(status).body(data);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public T getData() {
        return data;
    }
}

