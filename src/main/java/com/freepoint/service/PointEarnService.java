package com.freepoint.service;

import com.freepoint.domain.EffectivePolicy;
import com.freepoint.domain.LotSource;
import com.freepoint.domain.PointLot;
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
import java.time.temporal.ChronoUnit;
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

        EffectivePolicy policy = policyService.getEffectivePolicy(command.userId(), now);
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
        Long userId = transactionRepository.findUserIdByPointKey(command.pointKey())
                .orElseThrow(() -> new PointException(ErrorCode.TRANSACTION_NOT_FOUND));
        lockAccount(userId);

        // 거래와 Lot 은 락 획득 후 조회해 다른 트랜잭션의 변경이 반영된 최신 상태로 판정한다.
        PointTransaction earn = transactionRepository.findByPointKey(command.pointKey()).orElseThrow();
        if (!earn.isType(TransactionType.EARN)) {
            throw new PointException(ErrorCode.NOT_EARN_TRANSACTION);
        }

        Optional<PointTransaction> duplicated = transactionRepository.findByRequestId(command.requestId());
        if (duplicated.isPresent()) {
            return replayEarnCancel(duplicated.get(), earn);
        }

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

    /**
     * 같은 requestId 라도 요청 내용이 다르면 거부한다. 만료일은 요청에 있을 때만 비교한다.
     * 생략하면 기본 만료 기간을 요청 시각 기준으로 계산하므로 재시도 시점에 다시 계산한 값과 다르기 때문이다.
     */
    private EarnResult replayEarn(PointTransaction existing, EarnCommand command) {
        if (!existing.isType(TransactionType.EARN) || !Objects.equals(existing.getUserId(), command.userId())
                || existing.getAmount() != command.amount()) {
            throw new PointException(ErrorCode.DUPLICATE_REQUEST);
        }
        PointLot lot = lotRepository.findByTransactionId(existing.getId()).orElseThrow();
        if (lot.getSource() != command.source()
                || (command.expiresAt() != null
                        && !command.expiresAt().truncatedTo(ChronoUnit.SECONDS).equals(lot.getExpiresAt()))) {
            throw new PointException(ErrorCode.DUPLICATE_REQUEST);
        }
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
