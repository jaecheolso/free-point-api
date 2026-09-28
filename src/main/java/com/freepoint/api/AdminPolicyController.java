package com.freepoint.api;

import com.freepoint.api.dto.GlobalPolicyUpdateRequest;
import com.freepoint.api.dto.UserPolicyUpdateRequest;
import com.freepoint.service.GlobalPolicyResult;
import com.freepoint.service.PointPolicyService;
import com.freepoint.service.UserPolicyResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 정책", description = "1회 최대 적립 포인트 / 최대 보유 포인트 관리. 변경 시 이전 정책은 이력으로 남는다.")
@RestController
@RequestMapping("/api/admin/policies")
@RequiredArgsConstructor
public class AdminPolicyController {

    private final PointPolicyService policyService;

    @Operation(summary = "전역 정책 조회")
    @GetMapping("/global")
    public GlobalPolicyResult getGlobalPolicy() {
        return policyService.getGlobalPolicy();
    }

    @Operation(summary = "전역 정책 변경",
            description = "1회 최대 적립 포인트와 최대 보유 포인트를 변경한다. 즉시 적용되며, 만료 기간 등 나머지 항목은 유지된다.")
    @PutMapping("/global")
    public GlobalPolicyResult updateGlobalPolicy(@Valid @RequestBody GlobalPolicyUpdateRequest request) {
        return policyService.updateGlobalPolicy(request.maxEarnAmount(), request.maxHoldAmount());
    }

    @Operation(summary = "사용자 보유 한도 조회",
            description = "사용자에게 적용 중인 최대 보유 포인트. personal=false 면 전역 정책 값이다.")
    @GetMapping("/users/{userId}")
    public UserPolicyResult getUserPolicy(@PathVariable Long userId) {
        return policyService.getUserPolicy(userId);
    }

    @Operation(summary = "개인별 보유 한도 설정", description = "해당 사용자에게만 전역 보유 한도 대신 적용된다.")
    @PutMapping("/users/{userId}")
    public UserPolicyResult updateUserPolicy(@PathVariable Long userId,
                                             @Valid @RequestBody UserPolicyUpdateRequest request) {
        return policyService.updateUserMaxHold(userId, request.maxHoldAmount());
    }

    @Operation(summary = "개인별 보유 한도 해제", description = "해제 후에는 전역 보유 한도를 따른다.")
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> removeUserPolicy(@PathVariable Long userId) {
        policyService.removeUserPolicy(userId);
        return ResponseEntity.noContent().build();
    }
}
