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
     * SELECT ... FOR UPDATE. 현재 열린(effectiveTo 가 없는) 정책을 잠가 전역 정책 변경을 직렬화한다.
     * 락 대기 중 다른 변경이 커밋되면 잠그려던 행이 닫히므로 결과가 비어 있다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PointPolicy p where p.effectiveTo is null")
    Optional<PointPolicy> findOpenGlobalPolicyForUpdate();
}
