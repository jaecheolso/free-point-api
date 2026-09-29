package com.freepoint.repository;

import com.freepoint.domain.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    Optional<PointTransaction> findByPointKey(String pointKey);

    Optional<PointTransaction> findByRequestId(String requestId);

    /**
     * 락 획득 전에 잠글 계정만 알아내기 위한 조회. 엔티티를 미리 영속성 컨텍스트에 올리지 않아
     * 락 획득 후 조회하는 거래가 다른 트랜잭션의 변경을 반영한 최신 상태가 되도록 한다.
     */
    @Query("select t.userId from PointTransaction t where t.pointKey = :pointKey")
    Optional<Long> findUserIdByPointKey(String pointKey);

    /**
     * 사용 거래에 대해 cancelId 거래까지(포함) 누적된 사용취소 금액. 재시도 응답을 첫 응답 시점 기준으로 재구성할 때 쓴다.
     */
    @Query("""
            select coalesce(sum(t.amount), 0) from PointTransaction t
            where t.relatedTransactionId = :useId
              and t.type = com.freepoint.domain.TransactionType.USE_CANCEL
              and t.id <= :cancelId
            """)
    long sumUseCanceledAmountUpTo(Long useId, Long cancelId);
}
