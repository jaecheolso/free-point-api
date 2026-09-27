package com.freepoint.api;

import com.freepoint.api.dto.BalanceResponse;
import com.freepoint.api.dto.EarnCancelRequest;
import com.freepoint.api.dto.EarnRequest;
import com.freepoint.api.dto.UseCancelRequest;
import com.freepoint.api.dto.UseRequest;
import com.freepoint.service.EarnCancelResult;
import com.freepoint.service.EarnResult;
import com.freepoint.service.PointEarnService;
import com.freepoint.service.PointQueryService;
import com.freepoint.service.PointUseService;
import com.freepoint.service.UseCancelResult;
import com.freepoint.service.UseResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "포인트", description = "적립 / 적립취소 / 사용 / 사용취소 / 잔액 조회")
@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {

    private final PointEarnService earnService;
    private final PointUseService useService;
    private final PointQueryService queryService;

    @Operation(summary = "적립",
            description = "1회 적립 한도, 개인별 보유 한도, 만료일 범위(1일 이상 5년 미만)를 정책 기준으로 검증한다.")
    @PostMapping("/earn")
    public EarnResult earn(@Valid @RequestBody EarnRequest request) {
        return earnService.earn(request.toCommand());
    }

    @Operation(summary = "적립취소",
            description = "적립 금액 전체를 취소한다. 일부라도 사용되었거나 만료된 적립은 취소할 수 없다.")
    @PostMapping("/earn/{pointKey}/cancel")
    public EarnCancelResult cancelEarn(@PathVariable String pointKey,
                                       @Valid @RequestBody EarnCancelRequest request) {
        return earnService.cancelEarn(request.toCommand(pointKey));
    }

    @Operation(summary = "사용",
            description = "수기지급 우선 -> 만료일 짧게 남은 순 -> 적립순으로 차감하고, 차감 내역을 주문번호와 함께 기록한다.")
    @PostMapping("/use")
    public UseResult use(@Valid @RequestBody UseRequest request) {
        return useService.use(request.toCommand());
    }

    @Operation(summary = "사용취소",
            description = "사용 금액 중 전체 또는 일부를 취소한다. 사용된 적립이 이미 만료되었으면 그 금액만큼 신규 적립한다.")
    @PostMapping("/use/{pointKey}/cancel")
    public UseCancelResult cancelUse(@PathVariable String pointKey,
                                     @Valid @RequestBody UseCancelRequest request) {
        return useService.cancelUse(request.toCommand(pointKey));
    }

    @Operation(summary = "잔액 조회", description = "조회 시점 기준으로 만료된 포인트는 제외한 사용 가능 잔액.")
    @GetMapping("/users/{userId}/balance")
    public BalanceResponse balance(@PathVariable Long userId) {
        return new BalanceResponse(userId, queryService.getBalance(userId));
    }
}
