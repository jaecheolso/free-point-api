package com.freepoint.api.dto;

import com.freepoint.service.UseCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UseRequest(
        @Schema(example = "1") @NotNull Long userId,
        @Schema(description = "포인트를 사용한 주문번호", example = "A1234") @NotBlank @Size(max = 50) String orderNo,
        @Schema(example = "1200") @NotNull Long amount,
        @Schema(description = "요청 멱등 키", example = "use-20260927-0001") @NotBlank @Size(max = 64) String requestId,
        @Schema(example = "주문 결제") @Size(max = 200) String memo
) {

    public UseCommand toCommand() {
        return new UseCommand(userId, orderNo, amount, requestId, memo);
    }
}
