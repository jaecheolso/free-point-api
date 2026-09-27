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
 * 사용취소 상세. 사용 상세별로 원 Lot 복원 / 신규 재적립 여부를 기록한다.
 */
@Entity
@Table(name = "point_use_cancel_detail")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointUseCancelDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long cancelTransactionId;

    private Long useDetailId;

    private long amount;

    @Enumerated(EnumType.STRING)
    private RestoreType restoreType;

    private Long reissuedLotId;

    private LocalDateTime createdAt;
}
