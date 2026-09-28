package com.freepoint.repository;

import com.freepoint.domain.PointUserPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PointUserPolicyRepository extends JpaRepository<PointUserPolicy, Long> {

    @Query("""
            select p from PointUserPolicy p
            where p.userId = :userId
              and p.effectiveFrom <= :now
              and (p.effectiveTo is null or p.effectiveTo > :now)
            """)
    Optional<PointUserPolicy> findEffectiveUserPolicy(Long userId, LocalDateTime now);
}
