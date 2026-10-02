package org.petproject.dating_backend.common.exception;

public class UnauthorizedException extends BusinessException {
    public UnauthorizedException(int statusCode) {
        super(statusCode);
    }

    public UnauthorizedException(String message, int statusCode) {
        super(message, statusCode);
    }

    public UnauthorizedException(String message, Throwable cause, int statusCode) {
        super(message, cause, statusCode);
    }

    public UnauthorizedException(Throwable cause, int statusCode) {
        super(cause, statusCode);
    }
}
