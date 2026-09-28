package com.freepoint.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record GlobalPolicyUpdateRequest(
        @Schema(description = "1회 최대 적립 가능 포인트", example = "100000") @NotNull Long maxEarnAmount,
        @Schema(description = "개인별 최대 보유 가능 포인트 (개인 정책이 없는 사용자에게 적용)", example = "1000000")
        @NotNull Long maxHoldAmount
) {
}
