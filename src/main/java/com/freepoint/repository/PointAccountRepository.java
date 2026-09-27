package com.freepoint.repository;

import com.freepoint.domain.PointAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointAccountRepository extends JpaRepository<PointAccount, Long> {
}
