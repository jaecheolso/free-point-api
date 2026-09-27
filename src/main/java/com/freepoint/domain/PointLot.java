package com.freepoint.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 적립 단위. 잔액을 보유하는 유일한 엔티티.
 */
@Entity
@Table(name = "point_lot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long transactionId;

    private Long userId;

    private long originalAmount;

    private long remainingAmount;

    @Enumerated(EnumType.STRING)
    private LotSource source;

    private short usePriority;

    @Enumerated(EnumType.STRING)
    private LotOriginType originType;

    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    private LotStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
