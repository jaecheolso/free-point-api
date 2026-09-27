package com.freepoint.api;

import com.freepoint.api.dto.ManualEarnRequest;
import com.freepoint.service.EarnResult;
import com.freepoint.service.PointEarnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자", description = "관리자 수기지급")
@RestController
@RequestMapping("/api/admin/points")
@RequiredArgsConstructor
public class AdminPointController {

    private final PointEarnService earnService;

    @Operation(summary = "수기지급",
            description = "관리자가 수기로 지급한다. MANUAL 로 구분되어 사용 시 다른 적립보다 우선 사용된다.")
    @PostMapping("/manual-earn")
    public EarnResult manualEarn(@Valid @RequestBody ManualEarnRequest request) {
        return earnService.earn(request.toCommand());
    }
}
