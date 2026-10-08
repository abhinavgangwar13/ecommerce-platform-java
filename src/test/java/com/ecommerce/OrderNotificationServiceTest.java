package com.ecommerce;

import com.ecommerce.service.async.OrderNotificationService;
import com.ecommerce.service.async.OrderNotificationTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Focused test suite validating Core Java Multithreading and Synchronization:
 * 1. Concurrent execution of asynchronous tasks via ExecutorService and Thread pool.
 * 2. Thread-safety and mutual exclusion of shared state using explicit synchronization.
 * 3. Dedicated java.lang.Thread execution with java.lang.Runnable.
 */
public class OrderNotificationServiceTest {

    private OrderNotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new OrderNotificationService();
        notificationService.reset();
    }

    @AfterEach
    void tearDown() {
        if (notificationService != null) {
            notificationService.shutdown();
        }
    }

    @Test
    @DisplayName("Should demonstrate java.lang.Runnable interface implementation")
    void testOrderNotificationTaskImplementsRunnable() {
        OrderNotificationTask task = new OrderNotificationTask(1001L, "test@buyer.com", "CONFIRMATION", notificationService);

        // Core Java requirement: explicit Runnable demonstration
        assertTrue(task instanceof Runnable, "OrderNotificationTask must implement java.lang.Runnable");

        task.run();

        assertEquals(1, notificationService.getTotalNotificationsSent(), "Task execution should record exactly 1 notification");
        List<String> logs = notificationService.getNotificationAuditLog();
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("1001"), "Audit log must contain order ID 1001");
        assertTrue(logs.get(0).contains("test@buyer.com"), "Audit log must contain buyer email");
    }

    @Test
    @DisplayName("Should demonstrate direct java.lang.Thread creation, naming, and execution")
    void testDedicatedThreadCreationAndExecution() throws InterruptedException {
        Thread thread = notificationService.sendNotificationWithDedicatedThread(2001L, "direct_thread@buyer.com", "CONFIRMATION");

        assertNotNull(thread, "Thread instance must not be null");
        assertTrue(thread.getName().startsWith("Dedicated-Notification-Thread-"), "Thread should have a descriptive name");

        // Wait for thread to finish execution
        thread.join(5000);
        assertFalse(thread.isAlive(), "Thread must complete execution");

        assertEquals(1, notificationService.getTotalNotificationsSent(), "Counter must be incremented by dedicated thread");
        List<String> logs = notificationService.getNotificationAuditLog();
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("2001"));
    }

    @Test
    @DisplayName("Should demonstrate asynchronous execution via ExecutorService and CompletableFuture")
    void testAsyncExecutionViaExecutorService() throws Exception {
        CompletableFuture<Void> future1 = notificationService.sendOrderConfirmationAsync(3001L, "buyer1@demo.com");
        CompletableFuture<Void> future2 = notificationService.sendOrderCancellationAsync(3002L, "buyer2@demo.com");

        // Wait for both asynchronous tasks to complete
        CompletableFuture.allOf(future1, future2).get(5, TimeUnit.SECONDS);

        assertEquals(2, notificationService.getTotalNotificationsSent(), "Both async tasks should complete and increment counter");
        List<String> logs = notificationService.getNotificationAuditLog();
        assertEquals(2, logs.size());
    }

    @Test
    @DisplayName("Should prove thread safety and mutual exclusion on shared state under high concurrency (no race conditions)")
    void testConcurrentExecutionWithSynchronization() throws InterruptedException {
        int numberOfConcurrentThreads = 50;
        ExecutorService testPool = Executors.newFixedThreadPool(10);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(numberOfConcurrentThreads);

        for (int i = 1; i <= numberOfConcurrentThreads; i++) {
            final long orderId = 5000L + i;
            final String email = "buyer" + i + "@concurrent.com";

            testPool.submit(() -> {
                try {
                    // All threads wait at startGate to fire concurrently
                    startGate.await();
                    notificationService.recordNotification(orderId, email, "CONCURRENT_TEST");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneGate.countDown();
                }
            });
        }

        // Release all threads simultaneously to maximize contention on synchronized block
        startGate.countDown();

        boolean completedInTime = doneGate.await(10, TimeUnit.SECONDS);
        testPool.shutdown();

        assertTrue(completedInTime, "All concurrent tasks should complete within timeout");

        // Critical verification of synchronization:
        // Without synchronization, race conditions on non-atomic totalNotificationsSent++
        // and ArrayList.add() would lead to lost updates or ConcurrentModificationExceptions.
        assertEquals(numberOfConcurrentThreads, notificationService.getTotalNotificationsSent(),
                "Total notifications sent must match exact number of concurrent tasks without lost updates");

        List<String> auditLog = notificationService.getNotificationAuditLog();
        assertEquals(numberOfConcurrentThreads, auditLog.size(),
                "Audit log must contain all entries without race condition data loss");
    }
}
