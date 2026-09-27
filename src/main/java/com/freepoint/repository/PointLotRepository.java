package com.freepoint.repository;

import com.freepoint.domain.PointLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PointLotRepository extends JpaRepository<PointLot, Long> {

    Optional<PointLot> findByTransactionId(Long transactionId);

    /**
     * 사용 가능 잔액 = ACTIVE 이고 만료되지 않은 Lot 의 잔여 금액 합계.
     */
    @Query("""
            select coalesce(sum(l.remainingAmount), 0)
            from PointLot l
            where l.userId = :userId
              and l.status = com.freepoint.domain.LotStatus.ACTIVE
              and l.expiresAt > :now
            """)
    long sumUsableAmount(Long userId, LocalDateTime now);
}
