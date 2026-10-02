package org.petproject.dating_backend.common.exception;

public class NotFoundException extends BusinessException {
    public NotFoundException(int statusCode) {
        super(statusCode);
    }

    public NotFoundException(String message, int statusCode) {
        super(message, statusCode);
    }

    public NotFoundException(String message, Throwable cause, int statusCode) {
        super(message, cause, statusCode);
    }

    public NotFoundException(Throwable cause, int statusCode) {
        super(cause, statusCode);
    }
}
