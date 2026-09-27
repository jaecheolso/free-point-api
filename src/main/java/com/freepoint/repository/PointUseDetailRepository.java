package com.freepoint.repository;

import com.freepoint.domain.PointUseDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointUseDetailRepository extends JpaRepository<PointUseDetail, Long> {

    List<PointUseDetail> findByUseTransactionIdOrderBySeq(Long useTransactionId);
}
