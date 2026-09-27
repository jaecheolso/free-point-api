package com.freepoint.api.dto;

import com.freepoint.service.EarnCancelCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record EarnCancelRequest(
        @Schema(description = "요청 멱등 키", example = "earn-cancel-20260927-0001") @NotBlank String requestId,
        @Schema(example = "적립 오류 취소") String memo
) {

    public EarnCancelCommand toCommand(String pointKey) {
        return new EarnCancelCommand(pointKey, requestId, memo);
    }
}
