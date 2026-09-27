package com.freepoint.exception;

import org.springframework.http.HttpStatus;

/**
 * 400 : 요청 값 자체가 규칙에 맞지 않음
 * 404 : 대상이 존재하지 않음
 * 409 : 현재 상태 때문에 처리할 수 없음
 */
public enum ErrorCode {

    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "포인트 계정이 존재하지 않습니다."),
    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "포인트 거래가 존재하지 않습니다."),

    INVALID_EARN_AMOUNT(HttpStatus.BAD_REQUEST, "1회 적립 가능 금액 범위를 벗어났습니다."),
    INVALID_EXPIRES_AT(HttpStatus.BAD_REQUEST, "만료일이 허용 범위를 벗어났습니다."),
    GRANTED_BY_REQUIRED(HttpStatus.BAD_REQUEST, "수기지급은 지급자가 필요합니다."),
    NOT_EARN_TRANSACTION(HttpStatus.BAD_REQUEST, "적립 거래만 취소할 수 있습니다."),

    HOLD_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "최대 보유 가능 금액을 초과합니다."),
    ALREADY_CANCELED(HttpStatus.CONFLICT, "이미 취소된 적립입니다."),
    EXPIRED_LOT(HttpStatus.CONFLICT, "만료된 적립은 취소할 수 없습니다."),
    PARTIALLY_USED(HttpStatus.CONFLICT, "일부 사용된 적립은 취소할 수 없습니다."),
    DUPLICATE_REQUEST(HttpStatus.CONFLICT, "이미 다른 요청에 사용된 requestId 입니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}
