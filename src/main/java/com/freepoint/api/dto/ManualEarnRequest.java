package com.freepoint.api.dto;

import com.freepoint.domain.LotSource;
import com.freepoint.service.EarnCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ManualEarnRequest(
        @Schema(example = "1") @NotNull Long userId,
        @Schema(example = "1000") @NotNull Long amount,
        @Schema(description = "만료일시. 생략 시 기본 만료 기간(365일) 적용", example = "2027-03-01T23:59:59")
        LocalDateTime expiresAt,
        @Schema(description = "지급한 관리자", example = "admin01") @NotBlank @Size(max = 50) String grantedBy,
        @Schema(description = "요청 멱등 키", example = "manual-20260927-0001") @NotBlank @Size(max = 64) String requestId,
        @Schema(example = "CS 보상") @Size(max = 200) String memo
) {

    public EarnCommand toCommand() {
        return new EarnCommand(userId, amount, expiresAt, LotSource.MANUAL, grantedBy, requestId, memo);
    }
}
