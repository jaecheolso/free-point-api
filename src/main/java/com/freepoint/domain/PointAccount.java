package com.freepoint.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자별 포인트 계정. 잔액 변경 시 비관적 락 대상이 되는 단일 진입점.
 */
@Entity
@Table(name = "point_account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointAccount {

    @Id
    private Long userId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
