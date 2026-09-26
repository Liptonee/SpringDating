package org.petproject.dating_backend.common.exception;

public class ForbiddenException extends BusinessException{
    public ForbiddenException(int statusCode) {
        super(statusCode);
    }

    public ForbiddenException(String message, int statusCode) {
        super(message, statusCode);
    }

    public ForbiddenException(String message, Throwable cause, int statusCode) {
        super(message, cause, statusCode);
    }

    public ForbiddenException(Throwable cause, int statusCode) {
        super(cause, statusCode);
    }
}
