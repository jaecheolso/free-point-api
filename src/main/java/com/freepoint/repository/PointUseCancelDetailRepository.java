package com.freepoint.repository;

import com.freepoint.domain.PointUseCancelDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointUseCancelDetailRepository extends JpaRepository<PointUseCancelDetail, Long> {

    List<PointUseCancelDetail> findByCancelTransactionIdOrderById(Long cancelTransactionId);
}
