package com.freepoint.service;

import com.freepoint.domain.LotOriginType;
import com.freepoint.domain.LotSource;
import com.freepoint.domain.LotStatus;
import com.freepoint.domain.PointLot;
import com.freepoint.domain.PointTransaction;
import com.freepoint.domain.PointUseDetail;
import com.freepoint.domain.RestoreType;
import com.freepoint.domain.TransactionType;
import com.freepoint.exception.ErrorCode;
import com.freepoint.repository.PointLotRepository;
import com.freepoint.repository.PointTransactionRepository;
import com.freepoint.repository.PointUseDetailRepository;
import com.freepoint.support.IntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class PointUseServiceTest extends IntegrationTest {

    private static final long USER = 1L;

    @Autowired
    PointUseService useService;

    @Autowired
    PointEarnService earnService;

    @Autowired
    PointTransactionRepository transactionRepository;

    @Autowired
    PointLotRepository lotRepository;

    @Autowired
    PointUseDetailRepository useDetailRepository;

    @Nested
    class 사용 {

        @Test
        void 수기지급_포인트가_만료일과_관계없이_우선_사용된다() {
            String system = earn(LotSource.SYSTEM, 1000, now().plusDays(10));
            String manual = earn(LotSource.MANUAL, 1000, now().plusDays(300));

            UseResult result = useService.use(use(USER, "O-1", 500));

            assertThat(result.usedLots()).extracting(UsedLot::earnPointKey, UsedLot::amount)
                    .containsExactly(tuple(manual, 500L));
            assertThat(lotOf(system).getRemainingAmount()).isEqualTo(1000);
        }

        @Test
        void 같은_우선순위에서는_만료일이_짧게_남은_순서로_사용된다() {
            String later = earn(LotSource.SYSTEM, 1000, now().plusDays(300));
            String sooner = earn(LotSource.SYSTEM, 1000, now().plusDays(10));

            UseResult result = useService.use(use(USER, "O-1", 1500));

            assertThat(result.usedLots()).extracting(UsedLot::earnPointKey, UsedLot::amount)
                    .containsExactly(tuple(sooner, 1000L), tuple(later, 500L));
        }

        @Test
        void 만료일도_같으면_먼저_적립된_순서로_사용된다() {
            LocalDateTime expiresAt = now().plusDays(100);
            String first = earn(LotSource.SYSTEM, 1000, expiresAt);
            String second = earn(LotSource.SYSTEM, 1000, expiresAt);

            UseResult result = useService.use(use(USER, "O-1", 1200));

            assertThat(result.usedLots()).extracting(UsedLot::earnPointKey)
                    .containsExactly(first, second);
        }

        @Test
        void 사용_거래와_Lot_별_사용_상세가_주문번호와_함께_기록된다() {
            String a = earn(LotSource.SYSTEM, 1000, now().plusDays(10));
            String b = earn(LotSource.SYSTEM, 500, now().plusDays(20));

            UseResult result = useService.use(use(USER, "A1234", 1200));

            PointTransaction useTx = transactionRepository.findByPointKey(result.pointKey()).orElseThrow();
            assertThat(useTx.getType()).isEqualTo(TransactionType.USE);
            assertThat(useTx.getOrderNo()).isEqualTo("A1234");
            assertThat(useTx.getAmount()).isEqualTo(1200);

            List<PointUseDetail> details = useDetailRepository.findByUseTransactionIdOrderBySeq(useTx.getId());
            assertThat(details).extracting(PointUseDetail::getLotId, PointUseDetail::getAmount,
                            PointUseDetail::getSeq, PointUseDetail::getOrderNo)
                    .containsExactly(
                            tuple(lotOf(a).getId(), 1000L, 1, "A1234"),
                            tuple(lotOf(b).getId(), 200L, 2, "A1234"));

            assertThat(lotOf(a).getRemainingAmount()).isZero();
            assertThat(lotOf(b).getRemainingAmount()).isEqualTo(300);
            assertThat(balance()).isEqualTo(300);
        }

        @Test
        void 만료되었거나_취소된_포인트는_사용하지_않는다() {
            earn(LotSource.SYSTEM, 1000, now().plusDays(1));
            String canceled = earn(LotSource.MANUAL, 1000, now().plusDays(100));
            earnService.cancelEarn(new EarnCancelCommand(canceled, requestId(), null));
            String usable = earn(LotSource.SYSTEM, 1000, now().plusDays(100));
            clock.advance(Duration.ofDays(1));

            UseResult result = useService.use(use(USER, "O-1", 1000));

            assertThat(result.usedLots()).extracting(UsedLot::earnPointKey).containsExactly(usable);
        }

        @Test
        void 잔액이_부족하면_사용할_수_없고_아무것도_차감되지_않는다() {
            String a = earn(LotSource.SYSTEM, 1000, null);

            assertThatThrownBy(() -> useService.use(use(USER, "O-1", 1001)))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_BALANCE);
            assertThat(lotOf(a).getRemainingAmount()).isEqualTo(1000);
        }

        @Test
        void 사용금액은_1_이상이어야_한다() {
            earn(LotSource.SYSTEM, 1000, null);

            assertThatThrownBy(() -> useService.use(use(USER, "O-1", 0)))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_USE_AMOUNT);
        }

        @Test
        void 주문번호는_필수다() {
            earn(LotSource.SYSTEM, 1000, null);

            assertThatThrownBy(() -> useService.use(use(USER, " ", 100)))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NO_REQUIRED);
        }

        @Test
        void 존재하지_않는_계정은_사용할_수_없다() {
            assertThatThrownBy(() -> useService.use(use(999L, "O-1", 100)))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_NOT_FOUND);
        }

        @Test
        void 같은_requestId_로_다시_요청하면_기존_결과를_반환하고_중복_차감하지_않는다() {
            earn(LotSource.SYSTEM, 1000, null);
            UseCommand command = use(USER, "O-1", 300);

            UseResult first = useService.use(command);
            UseResult second = useService.use(command);

            assertThat(second.pointKey()).isEqualTo(first.pointKey());
            assertThat(second.usedLots()).isEqualTo(first.usedLots());
            assertThat(balance()).isEqualTo(700);
        }
    }

    @Nested
    class 사용취소 {

        @Test
        void 사용_순서대로_복원하고_부분취소_후_남은_금액만_추가로_취소할_수_있다() {
            String a = earn(LotSource.SYSTEM, 1000, now().plusDays(10));
            String b = earn(LotSource.SYSTEM, 500, now().plusDays(20));
            UseResult used = useService.use(use(USER, "O-1", 1200));

            UseCancelResult result = useService.cancelUse(cancel(used.pointKey(), 1100));

            assertThat(result.restoredLots())
                    .extracting(RestoredLot::earnPointKey, RestoredLot::amount, RestoredLot::restoreType)
                    .containsExactly(
                            tuple(a, 1000L, RestoreType.RESTORED),
                            tuple(b, 100L, RestoreType.RESTORED));
            assertThat(result.remainingCancelableAmount()).isEqualTo(100);
            assertThat(lotOf(a).getRemainingAmount()).isEqualTo(1000);
            assertThat(lotOf(b).getRemainingAmount()).isEqualTo(400);

            PointTransaction cancelTx = transactionRepository.findByPointKey(result.pointKey()).orElseThrow();
            PointTransaction useTx = transactionRepository.findByPointKey(used.pointKey()).orElseThrow();
            assertThat(cancelTx.getType()).isEqualTo(TransactionType.USE_CANCEL);
            assertThat(cancelTx.getRelatedTransactionId()).isEqualTo(useTx.getId());
            assertThat(cancelTx.getOrderNo()).isEqualTo("O-1");
            assertThat(useTx.getCanceledAmount()).isEqualTo(1100);

            assertThatThrownBy(() -> useService.cancelUse(cancel(used.pointKey(), 101)))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXCEEDS_CANCELABLE_AMOUNT);
            useService.cancelUse(cancel(used.pointKey(), 100));
            assertThat(balance()).isEqualTo(1500);
        }

        @Test
        void 만료된_Lot_에서_사용한_금액은_신규_적립한다() {
            String manual = earn(LotSource.MANUAL, 1000, now().plusDays(10));
            UseResult used = useService.use(use(USER, "O-1", 600));
            clock.advance(Duration.ofDays(10));

            UseCancelResult result = useService.cancelUse(cancel(used.pointKey(), 600));

            RestoredLot restored = result.restoredLots().getFirst();
            assertThat(restored.earnPointKey()).isEqualTo(manual);
            assertThat(restored.restoreType()).isEqualTo(RestoreType.REISSUED);

            PointTransaction reissueTx = transactionRepository.findByPointKey(restored.reissuedPointKey()).orElseThrow();
            assertThat(reissueTx.getType()).isEqualTo(TransactionType.EARN);
            assertThat(reissueTx.getAmount()).isEqualTo(600);
            assertThat(reissueTx.getRequestId()).isNull();

            PointLot reissued = lotRepository.findByTransactionId(reissueTx.getId()).orElseThrow();
            assertThat(reissued.getOriginType()).isEqualTo(LotOriginType.USE_CANCEL_REISSUE);
            assertThat(reissued.getSource()).isEqualTo(LotSource.MANUAL);
            assertThat(reissued.getExpiresAt()).isEqualTo(now().plusDays(365));

            assertThat(lotOf(manual).getRemainingAmount()).isEqualTo(400);
            assertThat(balance()).isEqualTo(600);
        }

        @Test
        void 재적립은_보유_한도의_제약을_받지_않는다() {
            earn(LotSource.SYSTEM, 1000, now().plusDays(1));
            UseResult used = useService.use(use(USER, "O-1", 1000));
            clock.advance(Duration.ofDays(1));
            for (int i = 0; i < 10; i++) {
                earn(LotSource.SYSTEM, 100_000, null);
            }

            useService.cancelUse(cancel(used.pointKey(), 1000));

            assertThat(balance()).isEqualTo(1_001_000);
        }

        @Test
        void 사용_거래가_아니면_사용취소할_수_없다() {
            String earn = earn(LotSource.SYSTEM, 1000, null);

            assertThatThrownBy(() -> useService.cancelUse(cancel(earn, 100)))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_USE_TRANSACTION);
        }

        @Test
        void 사용취소_금액은_1_이상이어야_한다() {
            earn(LotSource.SYSTEM, 1000, null);
            UseResult used = useService.use(use(USER, "O-1", 500));

            assertThatThrownBy(() -> useService.cancelUse(cancel(used.pointKey(), 0)))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CANCEL_AMOUNT);
        }

        @Test
        void 존재하지_않는_pointKey_는_사용취소할_수_없다() {
            assertThatThrownBy(() -> useService.cancelUse(cancel(UUID.randomUUID().toString(), 100)))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRANSACTION_NOT_FOUND);
        }

        @Test
        void 같은_requestId_로_다시_요청하면_기존_결과를_반환하고_중복_복원하지_않는다() {
            earn(LotSource.SYSTEM, 1000, null);
            UseResult used = useService.use(use(USER, "O-1", 500));
            UseCancelCommand command = cancel(used.pointKey(), 200);

            UseCancelResult first = useService.cancelUse(command);
            UseCancelResult second = useService.cancelUse(command);

            assertThat(second.pointKey()).isEqualTo(first.pointKey());
            assertThat(balance()).isEqualTo(700);
        }
    }

    @Nested
    class 사용과_적립취소 {

        @Test
        void 일부_사용된_적립은_취소할_수_없다() {
            String a = earn(LotSource.SYSTEM, 1000, null);
            useService.use(use(USER, "O-1", 1));

            assertThatThrownBy(() -> earnService.cancelEarn(new EarnCancelCommand(a, requestId(), null)))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARTIALLY_USED);
        }

        @Test
        void 사용분이_전부_사용취소되면_다시_적립취소할_수_있다() {
            String a = earn(LotSource.SYSTEM, 1000, null);
            UseResult used = useService.use(use(USER, "O-1", 300));
            useService.cancelUse(cancel(used.pointKey(), 300));

            earnService.cancelEarn(new EarnCancelCommand(a, requestId(), null));

            assertThat(lotOf(a).getStatus()).isEqualTo(LotStatus.CANCELED);
        }

        @Test
        void 사용취소로_재적립된_포인트도_적립취소할_수_있다() {
            earn(LotSource.SYSTEM, 1000, now().plusDays(1));
            UseResult used = useService.use(use(USER, "O-1", 1000));
            clock.advance(Duration.ofDays(1));
            UseCancelResult canceled = useService.cancelUse(cancel(used.pointKey(), 1000));
            String reissued = canceled.restoredLots().getFirst().reissuedPointKey();

            earnService.cancelEarn(new EarnCancelCommand(reissued, requestId(), null));

            assertThat(lotOf(reissued).getStatus()).isEqualTo(LotStatus.CANCELED);
            assertThat(balance()).isZero();
        }
    }

    private String earn(LotSource source, long amount, LocalDateTime expiresAt) {
        String grantedBy = source == LotSource.MANUAL ? "admin01" : null;
        return earnService.earn(new EarnCommand(USER, amount, expiresAt, source, grantedBy, requestId(), null))
                .pointKey();
    }

    private UseCommand use(long userId, String orderNo, long amount) {
        return new UseCommand(userId, orderNo, amount, requestId(), null);
    }

    private UseCancelCommand cancel(String usePointKey, long amount) {
        return new UseCancelCommand(usePointKey, amount, requestId(), null);
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
