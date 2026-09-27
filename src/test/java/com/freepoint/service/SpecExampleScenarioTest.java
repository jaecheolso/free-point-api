package com.freepoint.service;

import com.freepoint.domain.LotOriginType;
import com.freepoint.domain.LotSource;
import com.freepoint.domain.PointLot;
import com.freepoint.domain.PointTransaction;
import com.freepoint.domain.RestoreType;
import com.freepoint.repository.PointLotRepository;
import com.freepoint.repository.PointTransactionRepository;
import com.freepoint.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * 과제 명세 "4. 예시" 시나리오를 그대로 재현한다.
 */
class SpecExampleScenarioTest extends IntegrationTest {

    private static final long USER = 1L;

    @Autowired
    PointEarnService earnService;

    @Autowired
    PointUseService useService;

    @Autowired
    PointTransactionRepository transactionRepository;

    @Autowired
    PointLotRepository lotRepository;

    @Test
    void 명세_예시_A_부터_E_까지() {
        // 1. 1000원 적립한다 (총 잔액 0 -> 1000 원) : pointKey A
        //    (4번에서 A 만 만료되도록 A 의 만료일을 B 보다 짧게 준다)
        String a = earnService.earn(earn(1000, 10)).pointKey();
        assertThat(balance()).isEqualTo(1000);

        // 2. 500원 적립한다 (총 잔액 1000 -> 1500 원) : pointKey B
        String b = earnService.earn(earn(500, 365)).pointKey();
        assertThat(balance()).isEqualTo(1500);

        // 3. 주문번호 A1234 에서 1200원 사용한다 (총 잔액 1500 -> 300 원) : pointKey C
        UseResult c = useService.use(new UseCommand(USER, "A1234", 1200, requestId(), null));
        //    A 적립에서 1000원 사용 -> A 의 사용가능 잔액 1000 -> 0
        //    B 적립에서 200원 사용  -> B 의 사용가능 잔액 500 -> 300
        assertThat(c.usedLots()).extracting(UsedLot::earnPointKey, UsedLot::amount)
                .containsExactly(tuple(a, 1000L), tuple(b, 200L));
        assertThat(lotOf(a).getRemainingAmount()).isZero();
        assertThat(lotOf(b).getRemainingAmount()).isEqualTo(300);
        assertThat(balance()).isEqualTo(300);

        // 4. A 의 적립이 만료되었다
        clock.advance(Duration.ofDays(10));
        assertThat(lotOf(a).isExpired(now())).isTrue();

        // 5. C 의 사용금액 1200원 중 1100원을 부분 사용취소 한다 (총 잔액 300 -> 1400 원) : pointKey D
        UseCancelResult d = useService.cancelUse(new UseCancelCommand(c.pointKey(), 1100, requestId(), null));
        assertThat(d.restoredLots())
                .extracting(RestoredLot::earnPointKey, RestoredLot::amount, RestoredLot::restoreType)
                .containsExactly(
                        tuple(a, 1000L, RestoreType.REISSUED),
                        tuple(b, 100L, RestoreType.RESTORED));

        //    A 는 이미 만료되었으므로 pointKey E 로 1000원이 신규적립된다
        String e = d.restoredLots().getFirst().reissuedPointKey();
        PointLot eLot = lotOf(e);
        assertThat(eLot.getOriginalAmount()).isEqualTo(1000);
        assertThat(eLot.getOriginType()).isEqualTo(LotOriginType.USE_CANCEL_REISSUE);
        assertThat(lotOf(a).getRemainingAmount()).isZero();

        //    B 는 만료되지 않았으므로 사용가능 잔액 300 -> 400원
        assertThat(lotOf(b).getRemainingAmount()).isEqualTo(400);

        //    C 는 이제 1200원 사용금액 중 100원을 부분취소 할 수 있다
        assertThat(d.remainingCancelableAmount()).isEqualTo(100);

        assertThat(balance()).isEqualTo(1400);
        assertThat(List.of(a, b, c.pointKey(), d.pointKey(), e)).isSorted();
    }

    private EarnCommand earn(long amount, int expireDays) {
        return new EarnCommand(USER, amount, now().plusDays(expireDays), LotSource.SYSTEM, null, requestId(), null);
    }

    private PointLot lotOf(String earnPointKey) {
        PointTransaction tx = transactionRepository.findByPointKey(earnPointKey).orElseThrow();
        return lotRepository.findByTransactionId(tx.getId()).orElseThrow();
    }

    private long balance() {
        return lotRepository.sumUsableAmount(USER, now());
    }

    private String requestId() {
        return UUID.randomUUID().toString();
    }
}
