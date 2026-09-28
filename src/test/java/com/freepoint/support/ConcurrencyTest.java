package com.freepoint.support;

import com.freepoint.exception.ErrorCode;
import com.freepoint.exception.PointException;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;

/**
 * 동시성 테스트 공통 설정.
 * - 트랜잭션 롤백 방식으로는 여러 스레드가 커밋된 데이터를 다툴 수 없으므로 @Transactional 을 두지 않는다.
 * - 대신 테스트마다 schema.sql / data.sql 을 다시 실행해 초기 상태로 되돌린다.
 */
@SpringBootTest
@Import(IntegrationTest.ClockConfig.class)
@Sql(scripts = {"classpath:schema.sql", "classpath:data.sql"}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public abstract class ConcurrencyTest {

    @Autowired
    protected MutableClock clock;

    @BeforeEach
    void resetClock() {
        clock.setInstant(IntegrationTest.BASE_INSTANT);
    }

    protected LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * threadCount 개의 작업을 동시에 출발시키고 모두 끝날 때까지 기다린다. task 는 스레드 번호(0부터)를 받는다.
     */
    protected Outcome runConcurrently(int threadCount, IntConsumer task) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            int index = i;
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                task.accept(index);
                return null;
            }));
        }
        ready.await();
        start.countDown();

        List<Throwable> failures = new ArrayList<>();
        for (Future<?> future : futures) {
            try {
                future.get(30, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                failures.add(e.getCause());
            } catch (Exception e) {
                failures.add(e);
            }
        }
        executor.shutdownNow();
        return new Outcome(threadCount - failures.size(), failures);
    }

    /**
     * @param failures 실패한 작업의 예외. PointException 이 아닌 예외가 섞여 있으면 errorCodes() 가 실패한다.
     */
    protected record Outcome(int successCount, List<Throwable> failures) {

        public List<ErrorCode> errorCodes() {
            return failures.stream()
                    .map(failure -> {
                        if (failure instanceof PointException e) {
                            return e.getErrorCode();
                        }
                        throw new AssertionError("예상하지 못한 예외", failure);
                    })
                    .toList();
        }
    }
}
