package com.freepoint.service;

import com.freepoint.domain.LotOriginType;
import com.freepoint.domain.LotSource;
import com.freepoint.domain.LotStatus;
import com.freepoint.domain.PointLot;
import com.freepoint.domain.PointTransaction;
import com.freepoint.domain.TransactionType;
import com.freepoint.exception.ErrorCode;
import com.freepoint.exception.PointException;
import com.freepoint.repository.PointLotRepository;
import com.freepoint.repository.PointTransactionRepository;
import com.freepoint.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PointEarnServiceTest extends IntegrationTest {

    private static final long USER = 1L;

    @Autowired
    PointEarnService earnService;

    @Autowired
    PointTransactionRepository transactionRepository;

    @Autowired
    PointLotRepository lotRepository;

    @Autowired
    PointPolicyService policyService;

    @Autowired
    EntityManager entityManager;

    @Nested
    class 적립 {

        @Test
        void 적립하면_거래와_Lot_이_생성되고_잔액이_증가한다() {
            EarnResult result = earnService.earn(systemEarn(USER, 1000, null));

            PointTransaction tx = transactionRepository.findByPointKey(result.pointKey()).orElseThrow();
            assertThat(tx.getType()).isEqualTo(TransactionType.EARN);
            assertThat(tx.getAmount()).isEqualTo(1000);

            PointLot lot = lotRepository.findByTransactionId(tx.getId()).orElseThrow();
            assertThat(lot.getRemainingAmount()).isEqualTo(1000);
            assertThat(lot.getStatus()).isEqualTo(LotStatus.ACTIVE);
            assertThat(lot.getOriginType()).isEqualTo(LotOriginType.EARN);

            assertThat(balance(USER)).isEqualTo(1000);
        }

        @Test
        void 만료일을_지정하지_않으면_기본_365일이_적용된다() {
            EarnResult result = earnService.earn(systemEarn(USER, 1000, null));

            assertThat(result.expiresAt()).isEqualTo(now().plusDays(365));
        }

        @Test
        void 적립금액은_1_이상_100000_이하만_가능하다() {
            assertThatThrownBy(() -> earnService.earn(systemEarn(USER, 0, null)))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_EARN_AMOUNT);
            assertThatThrownBy(() -> earnService.earn(systemEarn(USER, 100_001, null)))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_EARN_AMOUNT);

            earnService.earn(systemEarn(USER, 1, null));
            earnService.earn(systemEarn(USER, 100_000, null));
            assertThat(balance(USER)).isEqualTo(100_001);
        }

        @Test
        void 변경된_1회_최대_적립액을_즉시_적용한다() {
            policyService.updateGlobalPolicy(5_000, 1_000_000);

            assertThatThrownBy(() -> earnService.earn(systemEarn(USER, 5_001, null)))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_EARN_AMOUNT);
            earnService.earn(systemEarn(USER, 5_000, null));
        }

        @Test
        void 보유_한도를_초과하면_적립할_수_없다() {
            for (int i = 0; i < 10; i++) {
                earnService.earn(systemEarn(USER, 100_000, null));
            }

            assertThatThrownBy(() -> earnService.earn(systemEarn(USER, 1, null)))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.HOLD_LIMIT_EXCEEDED);
        }

        @Test
        void 개인별_보유_한도를_적용한다() {
            policyService.updateUserMaxHold(USER, 3_000);
            earnService.earn(systemEarn(USER, 3_000, null));

            assertThatThrownBy(() -> earnService.earn(systemEarn(USER, 1, null)))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.HOLD_LIMIT_EXCEEDED);
        }

        @Test
        void 보유_한도_검사는_금액_합이_long_범위를_넘어도_한도_초과로_거절한다() {
            policyService.updateGlobalPolicy(Long.MAX_VALUE, Long.MAX_VALUE);
            earnService.earn(systemEarn(USER, Long.MAX_VALUE, null));

            assertThatThrownBy(() -> earnService.earn(systemEarn(USER, 1, null)))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.HOLD_LIMIT_EXCEEDED);
        }

        @Test
        void 만료된_포인트는_보유_한도_계산에서_제외된다() {
            for (int i = 0; i < 10; i++) {
                earnService.earn(systemEarn(USER, 100_000, now().plusDays(1)));
            }

            clock.advance(Duration.ofDays(1));

            assertThat(balance(USER)).isZero();
            earnService.earn(systemEarn(USER, 100_000, null));
        }

        @Test
        void 만료일은_최소_1일_이후여야_한다() {
            assertThatThrownBy(() -> earnService.earn(systemEarn(USER, 1000, now().plusDays(1).minusSeconds(1))))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_EXPIRES_AT);

            earnService.earn(systemEarn(USER, 1000, now().plusDays(1)));
        }

        @Test
        void 만료일은_5년_미만이어야_한다() {
            assertThatThrownBy(() -> earnService.earn(systemEarn(USER, 1000, now().plusYears(5))))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_EXPIRES_AT);

            earnService.earn(systemEarn(USER, 1000, now().plusYears(5).minusSeconds(1)));
        }

        @Test
        void 수기지급은_지급자를_기록하고_MANUAL_로_구분된다() {
            EarnResult result = earnService.earn(new EarnCommand(
                    USER, 1000, null, LotSource.MANUAL, "admin01", requestId(), "CS 보상"));

            PointTransaction tx = transactionRepository.findByPointKey(result.pointKey()).orElseThrow();
            assertThat(tx.getGrantedBy()).isEqualTo("admin01");

            PointLot lot = lotRepository.findByTransactionId(tx.getId()).orElseThrow();
            assertThat(lot.getSource()).isEqualTo(LotSource.MANUAL);
            assertThat(lot.getUsePriority()).isEqualTo((short) LotSource.MANUAL.usePriority());
        }

        @Test
        void 수기지급은_지급자가_필수다() {
            assertThatThrownBy(() -> earnService.earn(new EarnCommand(
                    USER, 1000, null, LotSource.MANUAL, null, requestId(), null)))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GRANTED_BY_REQUIRED);
        }

        @Test
        void 존재하지_않는_계정은_적립할_수_없다() {
            assertThatThrownBy(() -> earnService.earn(systemEarn(999L, 1000, null)))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_NOT_FOUND);
        }

        @Test
        void 같은_requestId_로_다시_요청하면_기존_결과를_반환하고_중복_적립하지_않는다() {
            String requestId = requestId();
            EarnCommand command = new EarnCommand(USER, 1000, null, LotSource.SYSTEM, null, requestId, null);

            EarnResult first = earnService.earn(command);
            EarnResult second = earnService.earn(command);

            assertThat(second.pointKey()).isEqualTo(first.pointKey());
            assertThat(balance(USER)).isEqualTo(1000);
        }

        @Test
        void 기본_만료일로_적립한_요청은_시간이_지난_뒤_재시도해도_기존_결과를_반환한다() {
            EarnCommand command = new EarnCommand(USER, 1000, null, LotSource.SYSTEM, null, requestId(), null);
            EarnResult first = earnService.earn(command);

            clock.advance(Duration.ofMinutes(1));
            EarnResult second = earnService.earn(command);

            assertThat(second.pointKey()).isEqualTo(first.pointKey());
        }

        @Test
        void 기본_만료일도_초_단위로_저장되어_재시도_응답이_첫_응답과_같다() {
            clock.setInstant(BASE_INSTANT.plusNanos(123_456_789));
            EarnCommand command = new EarnCommand(USER, 1000, null, LotSource.SYSTEM, null, requestId(), null);
            EarnResult first = earnService.earn(command);
            entityManager.flush();
            entityManager.clear();

            EarnResult second = earnService.earn(command);

            assertThat(second).isEqualTo(first);
        }

        @Test
        void 만료일은_초_단위로_저장되어_DB_에서_다시_읽어도_재시도가_기존_결과를_반환한다() {
            EarnCommand command = new EarnCommand(USER, 1000, now().plusDays(10).plusNanos(123_456_789),
                    LotSource.SYSTEM, null, requestId(), null);
            EarnResult first = earnService.earn(command);
            entityManager.flush();
            entityManager.clear();

            EarnResult second = earnService.earn(command);

            assertThat(first.expiresAt()).isEqualTo(now().plusDays(10));
            assertThat(second.pointKey()).isEqualTo(first.pointKey());
        }

        @Test
        void 같은_requestId_로_금액이_다르면_거부한다() {
            String requestId = requestId();
            earnService.earn(new EarnCommand(USER, 1000, null, LotSource.SYSTEM, null, requestId, null));

            assertThatThrownBy(() -> earnService.earn(
                    new EarnCommand(USER, 5000, null, LotSource.SYSTEM, null, requestId, null)))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_REQUEST);
            assertThat(balance(USER)).isEqualTo(1000);
        }

        @Test
        void 같은_requestId_로_수기지급_여부가_다르면_거부한다() {
            String requestId = requestId();
            earnService.earn(new EarnCommand(USER, 1000, null, LotSource.SYSTEM, null, requestId, null));

            assertThatThrownBy(() -> earnService.earn(
                    new EarnCommand(USER, 1000, null, LotSource.MANUAL, "admin01", requestId, null)))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_REQUEST);
        }

        @Test
        void 같은_requestId_로_만료일이_다르면_거부한다() {
            String requestId = requestId();
            earnService.earn(new EarnCommand(USER, 1000, now().plusDays(10), LotSource.SYSTEM, null, requestId, null));

            assertThatThrownBy(() -> earnService.earn(
                    new EarnCommand(USER, 1000, now().plusDays(20), LotSource.SYSTEM, null, requestId, null)))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_REQUEST);
        }

        @Test
        void 다른_사용자가_같은_requestId_를_쓰면_거부한다() {
            String requestId = requestId();
            earnService.earn(new EarnCommand(USER, 1000, null, LotSource.SYSTEM, null, requestId, null));

            assertThatThrownBy(() -> earnService.earn(
                    new EarnCommand(2L, 1000, null, LotSource.SYSTEM, null, requestId, null)))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_REQUEST);
        }
    }

    @Nested
    class 적립취소 {

        @Test
        void 적립을_취소하면_Lot_이_CANCELED_가_되고_취소_거래가_기록된다() {
            EarnResult earn = earnService.earn(systemEarn(USER, 1000, null));

            EarnCancelResult result = earnService.cancelEarn(new EarnCancelCommand(earn.pointKey(), requestId(), null));

            PointTransaction earnTx = transactionRepository.findByPointKey(earn.pointKey()).orElseThrow();
            PointTransaction cancelTx = transactionRepository.findByPointKey(result.pointKey()).orElseThrow();
            assertThat(cancelTx.getType()).isEqualTo(TransactionType.EARN_CANCEL);
            assertThat(cancelTx.getAmount()).isEqualTo(1000);
            assertThat(cancelTx.getRelatedTransactionId()).isEqualTo(earnTx.getId());
            assertThat(earnTx.getCanceledAmount()).isEqualTo(1000);

            PointLot lot = lotRepository.findByTransactionId(earnTx.getId()).orElseThrow();
            assertThat(lot.getStatus()).isEqualTo(LotStatus.CANCELED);
            assertThat(lot.getRemainingAmount()).isZero();

            assertThat(balance(USER)).isZero();
        }

        @Test
        void 이미_취소된_적립은_다시_취소할_수_없다() {
            EarnResult earn = earnService.earn(systemEarn(USER, 1000, null));
            earnService.cancelEarn(new EarnCancelCommand(earn.pointKey(), requestId(), null));

            assertThatThrownBy(() -> earnService.cancelEarn(new EarnCancelCommand(earn.pointKey(), requestId(), null)))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_CANCELED);
        }

        @Test
        void 만료된_적립은_취소할_수_없다() {
            EarnResult earn = earnService.earn(systemEarn(USER, 1000, now().plusDays(1)));
            clock.advance(Duration.ofDays(1));

            assertThatThrownBy(() -> earnService.cancelEarn(new EarnCancelCommand(earn.pointKey(), requestId(), null)))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXPIRED_LOT);
        }

        @Test
        void 적립_거래가_아니면_취소할_수_없다() {
            EarnResult earn = earnService.earn(systemEarn(USER, 1000, null));
            EarnCancelResult cancel = earnService.cancelEarn(new EarnCancelCommand(earn.pointKey(), requestId(), null));

            assertThatThrownBy(() -> earnService.cancelEarn(new EarnCancelCommand(cancel.pointKey(), requestId(), null)))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_EARN_TRANSACTION);
        }

        @Test
        void 존재하지_않는_pointKey_는_취소할_수_없다() {
            assertThatThrownBy(() -> earnService.cancelEarn(
                    new EarnCancelCommand(UUID.randomUUID().toString(), requestId(), null)))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRANSACTION_NOT_FOUND);
        }

        @Test
        void 같은_requestId_로_다시_취소하면_기존_결과를_반환한다() {
            EarnResult earn = earnService.earn(systemEarn(USER, 1000, null));
            EarnCancelCommand command = new EarnCancelCommand(earn.pointKey(), requestId(), null);

            EarnCancelResult first = earnService.cancelEarn(command);
            EarnCancelResult second = earnService.cancelEarn(command);

            assertThat(second.pointKey()).isEqualTo(first.pointKey());
        }
    }

    private EarnCommand systemEarn(long userId, long amount, LocalDateTime expiresAt) {
        return new EarnCommand(userId, amount, expiresAt, LotSource.SYSTEM, null, requestId(), null);
    }

    private String requestId() {
        return UUID.randomUUID().toString();
    }

    private long balance(long userId) {
        return lotRepository.sumUsableAmount(userId, now());
    }
}
