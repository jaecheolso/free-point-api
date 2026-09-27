package com.freepoint.repository;

import com.freepoint.domain.PointPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PointPolicyRepository extends JpaRepository<PointPolicy, Long> {

    @Query("""
            select p from PointPolicy p
            where p.scope = com.freepoint.domain.PolicyScope.USER
              and p.userId = :userId
              and p.effectiveFrom <= :now
              and (p.effectiveTo is null or p.effectiveTo > :now)
            """)
    Optional<PointPolicy> findEffectiveUserPolicy(Long userId, LocalDateTime now);

    @Query("""
            select p from PointPolicy p
            where p.scope = com.freepoint.domain.PolicyScope.GLOBAL
              and p.effectiveFrom <= :now
              and (p.effectiveTo is null or p.effectiveTo > :now)
            """)
    Optional<PointPolicy> findEffectiveGlobalPolicy(LocalDateTime now);
}
