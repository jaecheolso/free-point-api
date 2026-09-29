package com.freepoint.api.dto;

import com.freepoint.service.EarnCancelCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EarnCancelRequest(
        @Schema(description = "요청 멱등 키", example = "earn-cancel-20260927-0001") @NotBlank @Size(max = 64) String requestId,
        @Schema(example = "적립 오류 취소") @Size(max = 200) String memo
) {

    public EarnCancelCommand toCommand(String pointKey) {
        return new EarnCancelCommand(pointKey, requestId, memo);
    }
}
