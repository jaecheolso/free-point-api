package com.freepoint.service;

import com.freepoint.domain.PointLot;
import com.freepoint.domain.PointTransaction;
import com.freepoint.domain.PointUseCancelDetail;
import com.freepoint.domain.PointUseDetail;
import com.freepoint.domain.RestoreType;
import com.freepoint.domain.TransactionType;
import com.freepoint.exception.ErrorCode;
import com.freepoint.exception.PointException;
import com.freepoint.repository.PointAccountRepository;
import com.freepoint.repository.PointLotRepository;
import com.freepoint.repository.PointTransactionRepository;
import com.freepoint.repository.PointUseCancelDetailRepository;
import com.freepoint.repository.PointUseDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 사용 / 사용취소.
 * 모든 잔액 변경은 point_account 행 락을 먼저 획득한 뒤 수행한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PointUseService {

    private final PointAccountRepository accountRepository;
    private final PointTransactionRepository transactionRepository;
    private final PointLotRepository lotRepository;
    private final PointUseDetailRepository useDetailRepository;
    private final PointUseCancelDetailRepository useCancelDetailRepository;
    private final PointPolicyService policyService;
    private final Clock clock;

    /**
     * 수기지급 우선 -> 만료일 짧게 남은 순 -> 적립순으로 Lot 에서 차감하고,
     * Lot 별 차감 금액을 사용 상세로 남겨 1원 단위로 추적한다.
     */
    public UseResult use(UseCommand command) {
        if (command.amount() <= 0) {
            throw new PointException(ErrorCode.INVALID_USE_AMOUNT);
        }
        if (command.orderNo() == null || command.orderNo().isBlank()) {
            throw new PointException(ErrorCode.ORDER_NO_REQUIRED);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        lockAccount(command.userId());

        Optional<PointTransaction> duplicated = transactionRepository.findByRequestId(command.requestId());
        if (duplicated.isPresent()) {
            return replayUse(duplicated.get(), command);
        }

        List<PointLot> lots = lotRepository.findUsableLotsInUseOrder(command.userId(), now);
        long usable = lots.stream().mapToLong(PointLot::getRemainingAmount).sum();
        if (usable < command.amount()) {
            throw new PointException(ErrorCode.INSUFFICIENT_BALANCE,
                    "(사용 가능 " + usable + ", 요청 " + command.amount() + ")");
        }

        PointTransaction use = transactionRepository.save(PointTransaction.use(
                command.userId(), command.amount(), command.orderNo(), command.requestId(), command.memo(), now));

        List<UsedLot> usedLots = new ArrayList<>();
        long remaining = command.amount();
        int seq = 1;
        for (PointLot lot : lots) {
            if (remaining == 0) {
                break;
            }
            long portion = Math.min(remaining, lot.getRemainingAmount());
            lot.deduct(portion, now);
            useDetailRepository.save(PointUseDetail.of(use, lot, portion, seq++, now));
            usedLots.add(new UsedLot(earnPointKeyOf(lot), portion));
            remaining -= portion;
        }
        return new UseResult(use.getPointKey(), use.getUserId(), use.getOrderNo(), use.getAmount(), usedLots);
    }

    /**
     * 사용 상세를 사용 순서(seq)대로 되돌린다.
     * 원 Lot 이 유효하면 원 Lot 잔액으로 복원하고, 이미 만료되었으면 같은 금액을 신규 적립한다.
     * 재적립은 사용자가 이미 보유했던 포인트를 돌려주는 것이므로 적립/보유 한도를 적용하지 않는다.
     */
    public UseCancelResult cancelUse(UseCancelCommand command) {
        if (command.amount() <= 0) {
            throw new PointException(ErrorCode.INVALID_CANCEL_AMOUNT);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        Long userId = transactionRepository.findUserIdByPointKey(command.pointKey())
                .orElseThrow(() -> new PointException(ErrorCode.TRANSACTION_NOT_FOUND));
        lockAccount(userId);

        PointTransaction use = transactionRepository.findByPointKey(command.pointKey()).orElseThrow();
        if (!use.isType(TransactionType.USE)) {
            throw new PointException(ErrorCode.NOT_USE_TRANSACTION);
        }

        Optional<PointTransaction> duplicated = transactionRepository.findByRequestId(command.requestId());
        if (duplicated.isPresent()) {
            return replayUseCancel(duplicated.get(), use, command);
        }

        if (command.amount() > use.cancelableAmount()) {
            throw new PointException(ErrorCode.EXCEEDS_CANCELABLE_AMOUNT,
                    "(취소 가능 " + use.cancelableAmount() + ", 요청 " + command.amount() + ")");
        }

        PointTransaction cancel = transactionRepository.save(
                PointTransaction.useCancel(use, command.amount(), command.requestId(), command.memo(), now));
        use.cancel(command.amount());

        List<RestoredLot> restoredLots = new ArrayList<>();
        long remaining = command.amount();
        for (PointUseDetail detail : useDetailRepository.findByUseTransactionIdOrderBySeq(use.getId())) {
            if (remaining == 0) {
                break;
            }
            long portion = Math.min(remaining, detail.cancelableAmount());
            if (portion == 0) {
                continue;
            }
            detail.cancel(portion);
            PointLot lot = lotRepository.findById(detail.getLotId()).orElseThrow();

            if (lot.isExpired(now)) {
                PointLot reissued = reissue(lot, portion, now);
                useCancelDetailRepository.save(PointUseCancelDetail.reissued(cancel, detail, portion, reissued, now));
                restoredLots.add(new RestoredLot(earnPointKeyOf(lot), portion, RestoreType.REISSUED,
                        earnPointKeyOf(reissued)));
            } else {
                lot.restore(portion, now);
                useCancelDetailRepository.save(PointUseCancelDetail.restored(cancel, detail, portion, now));
                restoredLots.add(new RestoredLot(earnPointKeyOf(lot), portion, RestoreType.RESTORED, null));
            }
            remaining -= portion;
        }
        return new UseCancelResult(cancel.getPointKey(), use.getPointKey(), use.getUserId(), cancel.getAmount(),
                use.cancelableAmount(), restoredLots);
    }

    private PointLot reissue(PointLot expired, long amount, LocalDateTime now) {
        LocalDateTime expiresAt = now.plus(
                policyService.getEffectivePolicy(expired.getUserId(), now).defaultExpirePeriod());
        PointTransaction reissue = transactionRepository.save(
                PointTransaction.reissue(expired.getUserId(), amount, now));
        return lotRepository.save(PointLot.reissue(reissue, expired, expiresAt, now));
    }

    private void lockAccount(Long userId) {
        accountRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new PointException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    private String earnPointKeyOf(PointLot lot) {
        return transactionRepository.findById(lot.getTransactionId()).orElseThrow().getPointKey();
    }

    private UseResult replayUse(PointTransaction existing, UseCommand command) {
        if (!existing.isType(TransactionType.USE) || !Objects.equals(existing.getUserId(), command.userId())
                || existing.getAmount() != command.amount() || !existing.getOrderNo().equals(command.orderNo())) {
            throw new PointException(ErrorCode.DUPLICATE_REQUEST);
        }
        List<UsedLot> usedLots = useDetailRepository.findByUseTransactionIdOrderBySeq(existing.getId()).stream()
                .map(detail -> new UsedLot(
                        earnPointKeyOf(lotRepository.findById(detail.getLotId()).orElseThrow()), detail.getAmount()))
                .toList();
        return new UseResult(existing.getPointKey(), existing.getUserId(), existing.getOrderNo(),
                existing.getAmount(), usedLots);
    }

    private UseCancelResult replayUseCancel(PointTransaction existing, PointTransaction use, UseCancelCommand command) {
        if (!existing.isType(TransactionType.USE_CANCEL)
                || !Objects.equals(existing.getRelatedTransactionId(), use.getId())
                || existing.getAmount() != command.amount()) {
            throw new PointException(ErrorCode.DUPLICATE_REQUEST);
        }
        List<RestoredLot> restoredLots = useCancelDetailRepository
                .findByCancelTransactionIdOrderById(existing.getId()).stream()
                .map(cancelDetail -> {
                    PointUseDetail detail = useDetailRepository.findById(cancelDetail.getUseDetailId()).orElseThrow();
                    PointLot lot = lotRepository.findById(detail.getLotId()).orElseThrow();
                    String reissuedPointKey = cancelDetail.getReissuedLotId() == null ? null
                            : earnPointKeyOf(lotRepository.findById(cancelDetail.getReissuedLotId()).orElseThrow());
                    return new RestoredLot(earnPointKeyOf(lot), cancelDetail.getAmount(),
                            cancelDetail.getRestoreType(), reissuedPointKey);
                })
                .toList();
        return new UseCancelResult(existing.getPointKey(), use.getPointKey(), use.getUserId(), existing.getAmount(),
                use.cancelableAmount(), restoredLots);
    }
}
