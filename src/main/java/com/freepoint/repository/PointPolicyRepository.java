package com.freepoint.repository;

import com.freepoint.domain.PointPolicy;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PointPolicyRepository extends JpaRepository<PointPolicy, Long> {

    @Query("""
            select p from PointPolicy p
            where p.effectiveFrom <= :now
              and (p.effectiveTo is null or p.effectiveTo > :now)
            """)
    Optional<PointPolicy> findEffectiveGlobalPolicy(LocalDateTime now);

    /**
     * SELECT ... FOR UPDATE. 전역 정책 변경을 직렬화한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from PointPolicy p
            where p.effectiveFrom <= :now
              and (p.effectiveTo is null or p.effectiveTo > :now)
            """)
    Optional<PointPolicy> findEffectiveGlobalPolicyForUpdate(LocalDateTime now);
}
