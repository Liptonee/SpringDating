package org.petproject.dating_backend.common.exception;

public class ConflictException extends BusinessException{
    public ConflictException(int statusCode) {
        super(statusCode);
    }

    public ConflictException(String message, int statusCode) {
        super(message, statusCode);
    }

    public ConflictException(String message, Throwable cause, int statusCode) {
        super(message, cause, statusCode);
    }

    public ConflictException(Throwable cause, int statusCode) {
        super(cause, statusCode);
    }
}
