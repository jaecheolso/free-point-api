package com.freepoint.repository;

import com.freepoint.domain.PointLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
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

    /**
     * 사용 대상 Lot 을 사용 순서대로 조회한다. (idx_lot_use_order)
     * 수기지급 우선 -> 만료일 짧게 남은 순 -> 적립순
     */
    @Query("""
            select l from PointLot l
            where l.userId = :userId
              and l.status = com.freepoint.domain.LotStatus.ACTIVE
              and l.expiresAt > :now
              and l.remainingAmount > 0
            order by l.usePriority, l.expiresAt, l.id
            """)
    List<PointLot> findUsableLotsInUseOrder(Long userId, LocalDateTime now);
}
