package com.freepoint.repository;

import com.freepoint.domain.PointAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PointAccountRepository extends JpaRepository<PointAccount, Long> {

    /**
     * SELECT ... FOR UPDATE. 같은 사용자의 잔액 변경 요청을 직렬화한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from PointAccount a where a.userId = :userId")
    Optional<PointAccount> findByIdForUpdate(Long userId);
}
