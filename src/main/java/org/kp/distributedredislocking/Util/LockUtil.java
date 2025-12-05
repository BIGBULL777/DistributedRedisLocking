package org.kp.distributedredislocking.Util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LockUtil {

    private final RedisLockRegistry lockRegistry;
    private final RedisLockRegistry lockRegistryDistributed;
    private final Long waitTime;
    private final RestUtil restUtil;


    public LockUtil(RedisLockRegistry lockRegistry,
                    @Qualifier("lockRegistryDistributed")
                    RedisLockRegistry lockRegistryDistributed,
                    @Value("${redis.lock.acquire.wait.time}") Long waitTime,
                    RestUtil restUtil) {
        this.lockRegistry = lockRegistry;
        this.lockRegistryDistributed = lockRegistryDistributed;
        this.waitTime = waitTime;
        this.restUtil = restUtil;
    }

    /**
     * Simulates a transaction (e.g., updating a user's account) that must be
     * protected by a distributed lock using a unique resource identifier.
     *
     * @param resourceId A unique identifier (e.g., a user ID) to lock.
     * @return A status message.
     */
    public String acquireLock(String resourceId) {
        // 1. Obtain the specific Lock object for the resource ID.
        // This is the implementation of 'lockRegistry.obtain'.
        String lockKey = "resource-" + resourceId;
        Lock lock = lockRegistry.obtain(lockKey);

        // Parameters for tryLock: wait up to 100 milliseconds
        TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        boolean acquired = false;

        try {
            // 2. Attempt to acquire the lock in Redis.
            // This is the implementation of 'lock.tryLock'.
            log.info("Attempting to acquire lock for {}...", lockKey);
            acquired = lock.tryLock(waitTime, timeUnit);

            if (acquired) {
                log.info("SUCCESS: Lock acquired by thread {}. Starting critical section...", Thread.currentThread().getId());

                String responseFromVendor= restUtil.getDataFromExternalVendor();

                log.info("SUCCESS: Critical section finished. with response from vendor: {}", responseFromVendor);
                return String.format("Lock acquired and operation for %s completed successfully (waited %dms).", lockKey, waitTime);
            } else {
                // If acquired is false, the lock is already held by another JVM/thread.
                log.warn("FAILURE: Failed to acquire lock for {} within {}ms. Lock already held.", lockKey, waitTime);
                return String.format("FAILURE: Resource %s is currently busy. Please try again later.", lockKey);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Lock acquisition interrupted.", e);
            return "Operation interrupted.";
        } finally {
            // 3. IMPORTANT: Always release the lock in the finally block if it was acquired.
            if (acquired) {
                lock.unlock();
                log.info("Lock for {} released.", lockKey);
            }
        }
    }

    /**
     * Attempts to acquire a distributed lock for a unique resource ID,
     * performs a critical section (calling external vendor), and then releases the lock.
     *
     * @param resourceId A unique identifier (e.g., a user ID, order ID, product SKU) to lock.
     * @return A status message indicating success or failure to acquire the lock.
     */
    public String acquireLockInClusterMode(String resourceId) {
        // 1. Obtain the specific Lock object for the resource ID.
        String lockKey = "resource-" + resourceId;
        Lock lock = lockRegistryDistributed.obtain(lockKey);

        TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        boolean acquired = false;

        try {
            log.info("Attempting to acquire lock for {} (Wait Time: {}ms)...", lockKey, waitTime);
            acquired = lock.tryLock(waitTime, timeUnit);

            if (acquired) {
                log.info("SUCCESS: Lock acquired by thread {}. Starting critical section...", Thread.currentThread().getId());


                String responseFromVendor = restUtil.getDataFromExternalVendor();


                log.info("SUCCESS: Critical section finished. with response from vendor: {}", responseFromVendor);
                return String.format("Lock acquired and operation for %s completed successfully (waited %dms). Response: %s", lockKey, waitTime, responseFromVendor);
            } else {
                log.warn("FAILURE: Failed to acquire lock for {} within {}ms. Lock already held.", lockKey, waitTime);
                return String.format("FAILURE: Resource %s is currently busy. Please try again later.", lockKey);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Lock acquisition interrupted.", e);
            return "Operation interrupted.";
        } finally {
            // 3. IMPORTANT: Always release the lock in the finally block if it was acquired.
            if (acquired) {
                lock.unlock();
                log.info("Lock for {} released.", lockKey);
            }
        }
    }
}