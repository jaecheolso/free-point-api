package com.freepoint.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UserPolicyUpdateRequest(
        @Schema(description = "이 사용자의 최대 보유 가능 포인트", example = "3000") @NotNull Long maxHoldAmount
) {
}
