package org.kp.distributedredislocking;

import org.junit.jupiter.api.Test;
import org.kp.distributedredislocking.Util.LockUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Test to prove the distributed locking mechanism works
 * correctly when using the cluster-specific registry configuration (lockRegistryDistributed).
 * * NOTE: This requires a running Redis cluster (on ports 7000, 7001, 7002).
 */
@SpringBootTest
public class ClusterLockingIntegrationTest {

    // Inject the utility class containing the locking logic
    @Autowired
    private LockUtil lockUtil;

    // The unique ID all threads will try to lock
    private static final String RESOURCE_ID = "high_value_cluster_item_B";

    // Number of concurrent threads to simulate contention
    private static final int CONCURRENT_THREADS = 10;

    // Timeout for the entire test
    private static final int TEST_TIMEOUT_SECONDS = 30;

    @Test
    void testClusterDistributedLockingPreventsConcurrentAccess() throws InterruptedException {
        // Shared, thread-safe counter to track how many times the critical section was entered
        AtomicInteger successfulAcquisitions = new AtomicInteger(0);

        // Latch to wait for all threads to complete their execution
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);

        // A thread-safe list to collect all results (SUCCESS/FAILURE messages)
        List<String> results = Collections.synchronizedList(new LinkedList<>());

        // Executor service to run concurrent tasks
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);

        // 1. Submit 10 tasks, all targeting the SAME resource ID using the cluster method
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            executor.submit(() -> {
                String result = null;
                try {
                    // Call the new method that uses lockRegistryDistributed
                    result = lockUtil.acquireLockInClusterMode(RESOURCE_ID);

                    // Check if the service reported success
                    if (result.startsWith("Cluster Lock acquired")) {
                        successfulAcquisitions.incrementAndGet();
                    }
                } finally {
                    // Record the final result and count down the latch
                    if (result != null) {
                        results.add(result);
                    }
                    latch.countDown();
                }
            });
        }

        // 2. Wait for all threads to finish or timeout
        boolean allThreadsFinished = latch.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // 3. Cleanup the executor
        executor.shutdownNow();

        // --- ASSERTIONS ---

        // Verify all tasks actually ran
        assertThat(allThreadsFinished).isTrue()
                .withFailMessage("Test timed out before all threads finished execution.");

        // The critical assertion: ONLY ONE thread should have successfully acquired and executed
        assertThat(successfulAcquisitions.get())
                .isEqualTo(1)
                .withFailMessage("Expected exactly one successful lock acquisition, but found %d. The cluster locking mechanism failed.", successfulAcquisitions.get());

        // Verify the other 9 threads reported failure
        long failureCount = results.stream().filter(s -> s.startsWith("FAILURE")).count();
        assertThat(failureCount)
                .isEqualTo(CONCURRENT_THREADS - 1)
                .withFailMessage("Expected %d failures but found %d.", CONCURRENT_THREADS - 1, failureCount);
    }
}