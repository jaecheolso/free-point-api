package com.freepoint.service;

import com.freepoint.domain.EffectivePolicy;
import com.freepoint.exception.ErrorCode;
import com.freepoint.support.IntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PointPolicyServiceTest extends IntegrationTest {

    private static final long USER = 1L;
    private static final long OTHER_USER = 2L;

    @Autowired
    PointPolicyService policyService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Nested
    class 전역_정책 {

        @Test
        void 변경하면_기존_정책을_닫고_새_정책을_즉시_적용한다() {
            policyService.updateGlobalPolicy(50_000, 2_000_000);

            EffectivePolicy policy = policyService.getEffectivePolicy(USER, now());
            assertThat(policy.maxEarnAmount()).isEqualTo(50_000);
            assertThat(policy.maxHoldAmount()).isEqualTo(2_000_000);

            // 이력: 이전 행은 now 로 닫히고, 새 행이 now 부터 적용된다.
            assertThat(jdbcTemplate.queryForList(
                    "SELECT effective_to FROM point_policy ORDER BY id", LocalDateTime.class))
                    .containsExactly(now(), null);
        }

        @Test
        void 만료_기간과_최소_적립액은_이전_정책을_유지한다() {
            EffectivePolicy before = policyService.getEffectivePolicy(USER, now());

            policyService.updateGlobalPolicy(50_000, 2_000_000);

            EffectivePolicy after = policyService.getEffectivePolicy(USER, now());
            assertThat(after.minEarnAmount()).isEqualTo(before.minEarnAmount());
            assertThat(after.minExpirePeriod()).isEqualTo(before.minExpirePeriod());
            assertThat(after.maxExpirePeriod()).isEqualTo(before.maxExpirePeriod());
            assertThat(after.defaultExpirePeriod()).isEqualTo(before.defaultExpirePeriod());
        }

        @Test
        void 최대_적립액이_최소_적립액보다_작으면_변경할_수_없다() {
            assertThatThrownBy(() -> policyService.updateGlobalPolicy(0, 1_000_000))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_POLICY);
        }

        @Test
        void 보유_한도가_1_미만이면_변경할_수_없다() {
            assertThatThrownBy(() -> policyService.updateGlobalPolicy(100_000, 0))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_POLICY);
        }
    }

    @Nested
    class 개인_정책 {

        @Test
        void 설정하면_보유_한도만_전역_정책을_덮어쓴다() {
            policyService.updateUserMaxHold(USER, 3_000);

            EffectivePolicy policy = policyService.getEffectivePolicy(USER, now());
            assertThat(policy.maxHoldAmount()).isEqualTo(3_000);
            assertThat(policy.maxEarnAmount()).isEqualTo(100_000);
            assertThat(policyService.getEffectivePolicy(OTHER_USER, now()).maxHoldAmount()).isEqualTo(1_000_000);
        }

        @Test
        void 전역_정책을_변경하면_개인_정책_보유자에게도_반영된다() {
            policyService.updateUserMaxHold(USER, 3_000);

            policyService.updateGlobalPolicy(50_000, 2_000_000);

            EffectivePolicy policy = policyService.getEffectivePolicy(USER, now());
            assertThat(policy.maxEarnAmount()).isEqualTo(50_000);
            assertThat(policy.maxHoldAmount()).isEqualTo(3_000);
        }

        @Test
        void 다시_설정하면_이전_설정을_닫고_새_설정을_적용한다() {
            policyService.updateUserMaxHold(USER, 3_000);
            clock.advance(Duration.ofMinutes(1));

            policyService.updateUserMaxHold(USER, 5_000);

            assertThat(policyService.getEffectivePolicy(USER, now()).maxHoldAmount()).isEqualTo(5_000);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM point_user_policy WHERE user_id = ? AND effective_to IS NULL",
                    Long.class, USER)).isEqualTo(1);
        }

        @Test
        void 해제하면_전역_보유_한도로_돌아간다() {
            policyService.updateUserMaxHold(USER, 3_000);

            policyService.removeUserPolicy(USER);

            assertThat(policyService.getEffectivePolicy(USER, now()).maxHoldAmount()).isEqualTo(1_000_000);
        }

        @Test
        void 개인_정책이_없으면_해제할_수_없다() {
            assertThatThrownBy(() -> policyService.removeUserPolicy(USER))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_POLICY_NOT_FOUND);
        }

        @Test
        void 계정이_없는_사용자에게는_설정할_수_없다() {
            assertThatThrownBy(() -> policyService.updateUserMaxHold(999L, 3_000))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_NOT_FOUND);
        }

        @Test
        void 보유_한도가_1_미만이면_설정할_수_없다() {
            assertThatThrownBy(() -> policyService.updateUserMaxHold(USER, 0))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_POLICY);
        }
    }
}
