package com.freepoint.repository;

import com.freepoint.domain.PointPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
     * 아직 열려 있는(effectiveTo 가 없는) 경우에만 닫는다. 다른 변경이 먼저 닫았다면 0을 반환한다.
     * 락을 기다린 UPDATE 는 커밋된 최신 행으로 조건을 다시 평가하므로 H2 / MySQL 모두 같게 동작한다.
     */
    @Modifying
    @Query("update PointPolicy p set p.effectiveTo = :now where p.id = :id and p.effectiveTo is null")
    int closeIfOpen(Long id, LocalDateTime now);
}
