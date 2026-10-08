package com.ecommerce.service.async;

/**
 * Core Java Multithreading Task.
 * Implements java.lang.Runnable to represent an asynchronous background unit of work
 * for sending order notifications (order confirmation, cancellation, status updates).
 */
public class OrderNotificationTask implements Runnable {

    private final Long orderId;
    private final String recipientEmail;
    private final String notificationType;
    private final OrderNotificationService notificationService;

    public OrderNotificationTask(Long orderId, String recipientEmail, String notificationType, OrderNotificationService notificationService) {
        this.orderId = orderId;
        this.recipientEmail = recipientEmail;
        this.notificationType = notificationType;
        this.notificationService = notificationService;
    }

    @Override
    public void run() {
        // Asynchronously process the notification and record into thread-safe shared state
        notificationService.recordNotification(orderId, recipientEmail, notificationType);
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getNotificationType() {
        return notificationType;
    }
}
