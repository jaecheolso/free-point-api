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
}
