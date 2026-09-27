package com.freepoint.exception;

import lombok.Getter;

@Getter
public class PointException extends RuntimeException {

    private final ErrorCode errorCode;

    public PointException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    public PointException(ErrorCode errorCode, String detail) {
        super(errorCode.message() + " " + detail);
        this.errorCode = errorCode;
    }
}
