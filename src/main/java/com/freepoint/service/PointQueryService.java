package com.freepoint.service;

import com.freepoint.exception.ErrorCode;
import com.freepoint.exception.PointException;
import com.freepoint.repository.PointAccountRepository;
import com.freepoint.repository.PointLotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointQueryService {

    private final PointAccountRepository accountRepository;
    private final PointLotRepository lotRepository;
    private final Clock clock;

    /**
     * 사용 가능 잔액. 조회 시점 기준으로 만료된 포인트는 제외한다.
     */
    public long getBalance(Long userId) {
        if (!accountRepository.existsById(userId)) {
            throw new PointException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        return lotRepository.sumUsableAmount(userId, LocalDateTime.now(clock));
    }
}
