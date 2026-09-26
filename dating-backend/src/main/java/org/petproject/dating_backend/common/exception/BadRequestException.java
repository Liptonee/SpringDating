package org.petproject.dating_backend.common.exception;

public class BadRequestException extends BusinessException{

    public BadRequestException(int statusCode) {
        super(statusCode);
    }

    public BadRequestException(String message, int statusCode) {
        super(message, statusCode);
    }

    public BadRequestException(String message, Throwable cause, int statusCode) {
        super(message, cause, statusCode);
    }

    public BadRequestException(Throwable cause, int statusCode) {
        super(cause, statusCode);
    }
}
