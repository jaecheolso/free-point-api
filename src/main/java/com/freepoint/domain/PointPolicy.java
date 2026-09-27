package com.freepoint.domain;

import jakarta.persistence.Convert;
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
import java.time.Period;

/**
 * 포인트 정책. 변경 시 기존 행을 수정하지 않고 effectiveTo 로 닫은 뒤 새 행을 추가한다.
 */
@Entity
@Table(name = "point_policy")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private PolicyScope scope;

    private Long userId;

    private long minEarnAmount;

    private long maxEarnAmount;

    private long maxHoldAmount;

    @Convert(converter = PeriodConverter.class)
    private Period minExpirePeriod;

    @Convert(converter = PeriodConverter.class)
    private Period maxExpirePeriod;

    @Convert(converter = PeriodConverter.class)
    private Period defaultExpirePeriod;

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;

    private LocalDateTime createdAt;
}
