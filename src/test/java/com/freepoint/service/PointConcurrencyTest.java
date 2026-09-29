package com.freepoint.service;

import com.freepoint.domain.LotSource;
import com.freepoint.domain.RestoreType;
import com.freepoint.exception.ErrorCode;
import com.freepoint.repository.PointAccountRepository;
import com.freepoint.support.ConcurrencyTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 같은 사용자에 대한 요청이 동시에 들어와도 잔액/한도/취소 가능 금액 불변식이 지켜지는지 검증한다.
 */
class PointConcurrencyTest extends ConcurrencyTest {

    private static final long USER = 1L;
    private static final int THREADS = 10;

    @Autowired
    PointEarnService earnService;

    @Autowired
    PointUseService useService;

    @Autowired
    PointPolicyService policyService;

    @Autowired
    PointQueryService queryService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    PointAccountRepository accountRepository;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Test
    void 동시에_사용해도_잔액을_초과해_사용되지_않는다() throws InterruptedException {
        earn(5_000);

        Outcome outcome = runConcurrently(THREADS, i ->
                useService.use(new UseCommand(USER, "O-" + i, 1_000, requestId(), null)));

        assertThat(outcome.successCount()).isEqualTo(5);
        assertThat(outcome.errorCodes()).containsOnly(ErrorCode.INSUFFICIENT_BALANCE).hasSize(5);
        assertThat(queryService.getBalance(USER)).isZero();
    }

    @Test
    void 동시에_적립해도_보유_한도를_넘지_않는다() throws InterruptedException {
        policyService.updateUserMaxHold(USER, 5_000);

        Outcome outcome = runConcurrently(THREADS, i -> earn(1_000));

        assertThat(outcome.successCount()).isEqualTo(5);
        assertThat(outcome.errorCodes()).containsOnly(ErrorCode.HOLD_LIMIT_EXCEEDED).hasSize(5);
        assertThat(queryService.getBalance(USER)).isEqualTo(5_000);
    }

    @Test
    void 같은_requestId_로_동시에_적립하면_한_번만_적립된다() throws InterruptedException {
        EarnCommand command = new EarnCommand(USER, 1_000, null, LotSource.SYSTEM, null, requestId(), null);
        Set<String> pointKeys = ConcurrentHashMap.newKeySet();

        Outcome outcome = runConcurrently(THREADS, i -> pointKeys.add(earnService.earn(command).pointKey()));

        assertThat(outcome.successCount()).isEqualTo(THREADS);
        assertThat(pointKeys).hasSize(1);
        assertThat(queryService.getBalance(USER)).isEqualTo(1_000);
        assertThat(count("SELECT COUNT(*) FROM point_transaction WHERE request_id = ?", command.requestId()))
                .isEqualTo(1);
    }

    @Test
    void 동시에_사용취소해도_사용_금액을_초과해_취소되지_않는다() throws InterruptedException {
        earn(5_000);
        String useKey = useService.use(new UseCommand(USER, "O-1", 5_000, requestId(), null)).pointKey();

        Outcome outcome = runConcurrently(THREADS, i ->
                useService.cancelUse(new UseCancelCommand(useKey, 1_000, requestId(), null)));

        assertThat(outcome.successCount()).isEqualTo(5);
        assertThat(outcome.errorCodes()).containsOnly(ErrorCode.EXCEEDS_CANCELABLE_AMOUNT).hasSize(5);
        assertThat(queryService.getBalance(USER)).isEqualTo(5_000);
    }

    @Test
    void 적립취소와_사용이_동시에_요청되면_하나만_성공한다() throws InterruptedException {
        String earnKey = earn(1_000);

        Outcome outcome = runConcurrently(2, i -> {
            if (i == 0) {
                earnService.cancelEarn(new EarnCancelCommand(earnKey, requestId(), null));
            } else {
                useService.use(new UseCommand(USER, "O-1", 500, requestId(), null));
            }
        });

        assertThat(outcome.successCount()).isEqualTo(1);
        List<ErrorCode> errorCodes = outcome.errorCodes();
        if (errorCodes.contains(ErrorCode.PARTIALLY_USED)) {
            // 사용이 먼저 반영됨 -> 적립취소 거절
            assertThat(queryService.getBalance(USER)).isEqualTo(500);
        } else {
            // 적립취소가 먼저 반영됨 -> 사용 거절
            assertThat(errorCodes).containsExactly(ErrorCode.INSUFFICIENT_BALANCE);
            assertThat(queryService.getBalance(USER)).isZero();
        }
    }

    @Test
    void 전역_정책을_동시에_변경하면_먼저_커밋된_변경만_반영하고_나머지는_충돌로_거절한다() throws InterruptedException {
        Outcome outcome = runConcurrently(THREADS, i -> policyService.updateGlobalPolicy(50_000 + i, 1_000_000));

        assertThat(outcome.successCount()).isPositive();
        assertThat(outcome.errorCodes()).containsOnly(ErrorCode.DATA_CONFLICT);
        assertThat(count("SELECT COUNT(*) FROM point_policy WHERE effective_to IS NULL")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM point_policy")).isEqualTo(1 + outcome.successCount());
    }

    @Test
    void 개인_보유_한도를_동시에_설정해도_적용_중인_정책은_하나다() throws InterruptedException {
        Outcome outcome = runConcurrently(THREADS, i -> policyService.updateUserMaxHold(USER, 1_000L * (i + 1)));

        assertThat(outcome.failures()).isEmpty();
        assertThat(count("SELECT COUNT(*) FROM point_user_policy WHERE user_id = ? AND effective_to IS NULL", USER))
                .isEqualTo(1);
    }

    @Test
    void 락을_기다리는_동안_만료된_Lot_은_사용취소_시_신규_적립된다() throws Exception {
        earnService.earn(new EarnCommand(USER, 1_000, now().plusDays(1), LotSource.SYSTEM, null, requestId(), null));
        String useKey = useService.use(new UseCommand(USER, "O-1", 1_000, requestId(), null)).pointKey();

        UseCancelResult result = whileAccountLocked(
                () -> useService.cancelUse(new UseCancelCommand(useKey, 1_000, requestId(), null)),
                () -> clock.advance(Duration.ofDays(1)));

        assertThat(result.restoredLots()).extracting(RestoredLot::restoreType).containsExactly(RestoreType.REISSUED);
        assertThat(queryService.getBalance(USER)).isEqualTo(1_000);
    }

    @Test
    void 먼저_시각을_읽은_요청이_나중에_락을_얻어도_적용_중인_개인_정책은_하나다() throws Exception {
        whileAccountLocked(
                () -> policyService.updateUserMaxHold(USER, 3_000),
                () -> {
                    clock.advance(Duration.ofMinutes(1));
                    policyService.updateUserMaxHold(USER, 5_000);
                });

        assertThat(count("SELECT COUNT(*) FROM point_user_policy WHERE user_id = ? AND effective_to IS NULL", USER))
                .isEqualTo(1);
        assertThat(policyService.getUserPolicy(USER).maxHoldAmount()).isEqualTo(3_000);
    }

    /**
     * 계정 락을 먼저 잡은 트랜잭션 안에서 target 을 출발시켜 락을 기다리게 한 뒤,
     * whileWaiting 을 같은 트랜잭션에서 실행하고 커밋해 target 을 이어서 진행시킨다.
     */
    private <T> T whileAccountLocked(Supplier<T> target, Runnable whileWaiting) throws Exception {
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<Void> holder = CompletableFuture.runAsync(() -> transactionTemplate.executeWithoutResult(status -> {
            accountRepository.findByIdForUpdate(USER).orElseThrow();
            locked.countDown();
            await(release);
            whileWaiting.run();
        }));
        locked.await();

        CompletableFuture<T> result = CompletableFuture.supplyAsync(target);
        Thread.sleep(300); // target 이 계정 락 대기에 들어갈 시간
        release.countDown();

        holder.get(10, TimeUnit.SECONDS);
        return result.get(10, TimeUnit.SECONDS);
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private String earn(long amount) {
        return earnService.earn(new EarnCommand(USER, amount, null, LotSource.SYSTEM, null, requestId(), null))
                .pointKey();
    }

    private long count(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Long.class, args);
    }

    private String requestId() {
        return UUID.randomUUID().toString();
    }
}
