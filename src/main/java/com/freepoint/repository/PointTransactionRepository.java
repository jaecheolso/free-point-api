package com.freepoint.repository;

import com.freepoint.domain.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    Optional<PointTransaction> findByPointKey(String pointKey);

    Optional<PointTransaction> findByRequestId(String requestId);
}
