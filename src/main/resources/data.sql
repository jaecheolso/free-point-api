-- =====================================================================
-- 초기 데이터
--   - 전역 포인트 정책 1건 (요건 3-1-1 / 3-1-2 의 기본값)
--   - 데모용 계정. 운영에서는 회원 가입 시점에 생성된다.
-- =====================================================================

INSERT INTO point_policy
    (min_earn_amount, max_earn_amount, max_hold_amount,
     min_expire_period, max_expire_period, default_expire_period,
     effective_from,                      effective_to, created_at)
VALUES
    (1,               100000,          1000000,
     'P1D',             'P5Y',             'P365D',
     TIMESTAMP '2000-01-01 00:00:00',     NULL,         CURRENT_TIMESTAMP);

INSERT INTO point_account (user_id, created_at, updated_at) VALUES
    (1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
