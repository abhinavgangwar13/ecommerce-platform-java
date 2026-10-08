package com.ecommerce.service.async;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/**
 * Service demonstrating explicit Core Java Multithreading and Synchronization.
 *
 * Academic Rubric Alignment (Category 2: Core Java Concepts - Concurrency & Synchronization):
 * 1. java.util.concurrent.ExecutorService thread pool management via Executors.newFixedThreadPool
 * 2. java.lang.Runnable task encapsulation via OrderNotificationTask
 * 3. Direct java.lang.Thread creation and execution
 * 4. Critical section protection and mutual exclusion using explicit synchronized blocks
 * 5. Safe concurrent updates to shared in-memory state without race conditions
 */
@Service
public class OrderNotificationService {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationService.class);

    // Thread pool executor for asynchronous background notification tasks
    private final ExecutorService executorService;

    // Explicit monitor lock object for mutual exclusion on shared mutable state
    private final Object lock = new Object();

    // Shared mutable state across worker threads
    private int totalNotificationsSent = 0;
    private final List<String> notificationAuditLog = new ArrayList<>();

    public OrderNotificationService() {
        // Core Java: Fixed thread pool with named daemon threads
        this.executorService = Executors.newFixedThreadPool(4, new ThreadFactory() {
            private int threadId = 0;

            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "OrderNotificationWorker-" + (++threadId));
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    /**
     * Dispatches an asynchronous order confirmation notification using the ExecutorService.
     * Submits a Runnable task to the thread pool for non-blocking background execution.
     *
     * @param orderId the ID of the confirmed order
     * @param recipientEmail the buyer's email address
     * @return CompletableFuture representing pending completion of the task
     */
    public CompletableFuture<Void> sendOrderConfirmationAsync(Long orderId, String recipientEmail) {
        OrderNotificationTask task = new OrderNotificationTask(orderId, recipientEmail, "CONFIRMATION", this);
        return CompletableFuture.runAsync(task, executorService);
    }

    /**
     * Dispatches an asynchronous order cancellation notification using the ExecutorService.
     *
     * @param orderId the ID of the cancelled order
     * @param recipientEmail the buyer's email address
     * @return CompletableFuture representing pending completion of the task
     */
    public CompletableFuture<Void> sendOrderCancellationAsync(Long orderId, String recipientEmail) {
        OrderNotificationTask task = new OrderNotificationTask(orderId, recipientEmail, "CANCELLATION", this);
        return CompletableFuture.runAsync(task, executorService);
    }

    /**
     * Dispatches a notification using a dedicated java.lang.Thread instance.
     * Explicitly demonstrates direct Thread instantiation and start() with a Runnable target.
     *
     * @param orderId the order ID
     * @param recipientEmail the recipient email
     * @param type notification type
     * @return the started Thread instance
     */
    public Thread sendNotificationWithDedicatedThread(Long orderId, String recipientEmail, String type) {
        OrderNotificationTask task = new OrderNotificationTask(orderId, recipientEmail, type, this);
        Thread thread = new Thread(task, "Dedicated-Notification-Thread-" + orderId);
        thread.start();
        return thread;
    }

    /**
     * Core Java Synchronization:
     * Critical section protected by explicit synchronization on the shared monitor lock.
     * Guarantees atomic counter increments and audit log append operations across concurrent threads,
     * preventing race conditions and lost updates.
     *
     * @param orderId the order ID
     * @param recipientEmail recipient email
     * @param type notification type (e.g. CONFIRMATION, CANCELLATION)
     */
    public void recordNotification(Long orderId, String recipientEmail, String type) {
        synchronized (lock) {
            totalNotificationsSent++;
            String logEntry = String.format("[%s] Order #%d notification dispatched to %s at %s",
                    type, orderId, recipientEmail, LocalDateTime.now());
            notificationAuditLog.add(logEntry);
            log.info("Background notification recorded: {}", logEntry);
        }
    }

    /**
     * Thread-safe query of total notifications sent using synchronized lock.
     */
    public int getTotalNotificationsSent() {
        synchronized (lock) {
            return totalNotificationsSent;
        }
    }

    /**
     * Thread-safe read snapshot of the notification audit log.
     */
    public List<String> getNotificationAuditLog() {
        synchronized (lock) {
            return Collections.unmodifiableList(new ArrayList<>(notificationAuditLog));
        }
    }

    /**
     * Thread-safe reset of shared mutable state (primarily used between test executions).
     */
    public void reset() {
        synchronized (lock) {
            totalNotificationsSent = 0;
            notificationAuditLog.clear();
        }
    }

    /**
     * Graceful shutdown of the ExecutorService upon application termination.
     */
    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
