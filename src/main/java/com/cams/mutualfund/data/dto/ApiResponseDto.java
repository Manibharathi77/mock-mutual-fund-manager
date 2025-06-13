package com.cams.mutualfund.data.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponseDto<T> {

    private boolean success;
    private int status;
    private String message;
    private LocalDateTime timestamp;
    private T data;
    private Map<String, String> fieldErrors;

    // Constructor for success responses with data
    public ApiResponseDto(int status, String message, T data) {
        this.success = true;
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.data = data;
    }

    // Constructor for success responses without data
    public ApiResponseDto(int status, String message) {
        this.success = true;
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    // Constructor for error responses
    public ApiResponseDto(boolean success, int status, String message) {
        this.success = success;
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.data = null;
        this.fieldErrors = new HashMap<>();
    }

    // Constructor for validation errors
    public ApiResponseDto(int status, String message, Map<String, String> fieldErrors) {
        this.success = false;
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.data = null;
        this.fieldErrors = fieldErrors;
    }

    // Getters and setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(Map<String, String> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }

    public void addFieldError(String field, String errorMessage) {
        if (this.fieldErrors == null) {
            this.fieldErrors = new HashMap<>();
        }
        this.fieldErrors.put(field, errorMessage);
    }

    // Static factory methods for common responses
    public static <T> ApiResponseDto<T> ok(String message, T data) {
        return new ApiResponseDto<>(200, message, data);
    }

    public static <T> ApiResponseDto<T> created(String message, T data) {
        return new ApiResponseDto<>(201, message, data);
    }

    public static ApiResponseDto<?> success(String message) {
        return new ApiResponseDto<>(200, message);
    }

    public static ApiResponseDto<?> error(int status, String message) {
        return new ApiResponseDto<>(false, status, message);
    }

    public static ApiResponseDto<?> validationError(String message, Map<String, String> fieldErrors) {
        return new ApiResponseDto<>(400, message, fieldErrors);
    }
}
