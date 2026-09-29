package com.freepoint.api.dto;

import com.freepoint.domain.LotSource;
import com.freepoint.service.EarnCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 적립 금액 범위와 만료일 범위는 정책(point_policy) 값으로 서비스에서 검증한다.
 */
public record EarnRequest(
        @Schema(example = "1") @NotNull Long userId,
        @Schema(example = "1000") @NotNull Long amount,
        @Schema(description = "만료일시. 생략 시 기본 만료 기간(365일) 적용", example = "2027-03-01T23:59:59")
        LocalDateTime expiresAt,
        @Schema(description = "요청 멱등 키. 같은 값으로 재요청 시 기존 결과 반환", example = "earn-20260927-0001")
        @NotBlank @Size(max = 64) String requestId,
        @Schema(example = "주문 적립") @Size(max = 200) String memo
) {

    public EarnCommand toCommand() {
        return new EarnCommand(userId, amount, expiresAt, LotSource.SYSTEM, null, requestId, memo);
    }
}
