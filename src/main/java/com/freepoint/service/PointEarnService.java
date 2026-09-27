package com.freepoint.service;

import com.freepoint.domain.LotSource;
import com.freepoint.domain.PointLot;
import com.freepoint.domain.PointPolicy;
import com.freepoint.domain.PointTransaction;
import com.freepoint.domain.TransactionType;
import com.freepoint.exception.ErrorCode;
import com.freepoint.exception.PointException;
import com.freepoint.repository.PointAccountRepository;
import com.freepoint.repository.PointLotRepository;
import com.freepoint.repository.PointTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * 적립 / 적립취소.
 * 모든 잔액 변경은 point_account 행 락을 먼저 획득한 뒤 수행한다.
 * requestId 중복 확인도 락 하위에서 해야 같은 요청이 동시에 들어와도 한 번만 처리된다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PointEarnService {

    private final PointAccountRepository accountRepository;
    private final PointTransactionRepository transactionRepository;
    private final PointLotRepository lotRepository;
    private final PointPolicyService policyService;
    private final Clock clock;

    public EarnResult earn(EarnCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        lockAccount(command.userId());

        Optional<PointTransaction> duplicated = transactionRepository.findByRequestId(command.requestId());
        if (duplicated.isPresent()) {
            return replayEarn(duplicated.get(), command);
        }

        if (command.source() == LotSource.MANUAL && (command.grantedBy() == null || command.grantedBy().isBlank())) {
            throw new PointException(ErrorCode.GRANTED_BY_REQUIRED);
        }

        PointPolicy policy = policyService.getEffectivePolicy(command.userId(), now);
        policy.validateEarnAmount(command.amount());
        LocalDateTime expiresAt = policy.resolveExpiresAt(command.expiresAt(), now);
        policy.validateHoldLimit(lotRepository.sumUsableAmount(command.userId(), now), command.amount());

        PointTransaction earn = transactionRepository.save(PointTransaction.earn(
                command.userId(), command.amount(), command.requestId(), command.grantedBy(), command.memo(), now));
        PointLot lot = lotRepository.save(PointLot.earn(earn, command.source(), expiresAt, now));
        return EarnResult.of(earn, lot);
    }

    public EarnCancelResult cancelEarn(EarnCancelCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        PointTransaction earn = transactionRepository.findByPointKey(command.pointKey())
                .orElseThrow(() -> new PointException(ErrorCode.TRANSACTION_NOT_FOUND));
        if (!earn.isType(TransactionType.EARN)) {
            throw new PointException(ErrorCode.NOT_EARN_TRANSACTION);
        }
        lockAccount(earn.getUserId());

        Optional<PointTransaction> duplicated = transactionRepository.findByRequestId(command.requestId());
        if (duplicated.isPresent()) {
            return replayEarnCancel(duplicated.get(), earn);
        }

        // Lot 은 락 획득 후 처음 조회하므로 최신 상태다. 취소 가능 여부는 Lot 기준으로 판정한다.
        PointLot lot = lotRepository.findByTransactionId(earn.getId())
                .orElseThrow(() -> new IllegalStateException("적립 거래에 Lot 이 없습니다. transactionId=" + earn.getId()));
        lot.cancel(now);
        earn.cancel(earn.getAmount());

        PointTransaction cancel = transactionRepository.save(
                PointTransaction.earnCancel(earn, command.requestId(), command.memo(), now));
        return EarnCancelResult.of(cancel, earn);
    }

    private void lockAccount(Long userId) {
        accountRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new PointException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    private EarnResult replayEarn(PointTransaction existing, EarnCommand command) {
        if (!existing.isType(TransactionType.EARN) || !Objects.equals(existing.getUserId(), command.userId())) {
            throw new PointException(ErrorCode.DUPLICATE_REQUEST);
        }
        PointLot lot = lotRepository.findByTransactionId(existing.getId()).orElseThrow();
        return EarnResult.of(existing, lot);
    }

    private EarnCancelResult replayEarnCancel(PointTransaction existing, PointTransaction earn) {
        if (!existing.isType(TransactionType.EARN_CANCEL)
                || !Objects.equals(existing.getRelatedTransactionId(), earn.getId())) {
            throw new PointException(ErrorCode.DUPLICATE_REQUEST);
        }
        return EarnCancelResult.of(existing, earn);
    }
}
