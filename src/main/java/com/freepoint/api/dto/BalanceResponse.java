package com.freepoint.api.dto;

public record BalanceResponse(
        Long userId,
        long balance
) {
}
