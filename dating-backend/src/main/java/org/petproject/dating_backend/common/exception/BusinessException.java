package org.petproject.dating_backend.common.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BusinessException extends RuntimeException {
    private final int statusCode;

    public BusinessException(int statusCode) {
        this.statusCode = statusCode;
    }

    public BusinessException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public BusinessException(String message, Throwable cause, int statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public BusinessException(Throwable cause, int statusCode) {
        super(cause);
        this.statusCode = statusCode;
    }


}
