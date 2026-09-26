package org.petproject.dating_backend.common.exception;


public class PhotoException extends BusinessException {

    public PhotoException(int statusCode) {
        super(statusCode);
    }

    public PhotoException(String message, int statusCode) {
        super(message, statusCode);
    }

    public PhotoException(String message, Throwable cause, int statusCode) {
        super(message, cause, statusCode);
    }

    public PhotoException(Throwable cause, int statusCode) {
        super(cause, statusCode);
    }
}
