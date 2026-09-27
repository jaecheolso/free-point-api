package com.freepoint.api.dto;

import com.freepoint.service.UseCancelCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UseCancelRequest(
        @Schema(description = "사용취소 금액 (사용 금액 중 전체 또는 일부)", example = "1100") @NotNull Long amount,
        @Schema(description = "요청 멱등 키", example = "use-cancel-20260927-0001") @NotBlank String requestId,
        @Schema(example = "부분 환불") String memo
) {

    public UseCancelCommand toCommand(String pointKey) {
        return new UseCancelCommand(pointKey, amount, requestId, memo);
    }
}
