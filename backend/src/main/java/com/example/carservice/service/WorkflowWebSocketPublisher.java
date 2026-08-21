package com.example.carservice.service;

import com.example.carservice.dto.NotificationResponse;
import com.example.carservice.dto.WorkflowEventMessage;
import com.example.carservice.entity.WorkflowStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * WorkflowWebSocketPublisher
 *
 * Broadcasts real-time service workflow milestones and notification alerts over STOMP WebSocket topics.
 */
@Service
public class WorkflowWebSocketPublisher {

    private static final Logger log = LoggerFactory.getLogger(WorkflowWebSocketPublisher.class);

    private final SimpMessagingTemplate messagingTemplate;

    public WorkflowWebSocketPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Broadcasts a service workflow status update to clients subscribed to a specific booking.
     */
    public void publishWorkflowUpdate(Long bookingId, WorkflowStatus status, String message) {
        try {
            WorkflowEventMessage event = new WorkflowEventMessage(
                    "SERVICE_STATUS_UPDATED",
                    bookingId,
                    status,
                    message
            );
            String destination = "/topic/workflow/" + bookingId;
            messagingTemplate.convertAndSend(destination, event);
            log.info("WebSocket Event published to {}: status={}", destination, status);
        } catch (Exception e) {
            log.warn("Failed to publish WebSocket workflow event for booking {}: {}", bookingId, e.getMessage());
        }
    }

    /**
     * Broadcasts an in-app notification to a user's notification channel.
     */
    public void publishNotification(Long recipientId, NotificationResponse notification) {
        try {
            String destination = "/topic/notifications/" + recipientId;
            messagingTemplate.convertAndSend(destination, notification);
            log.info("WebSocket Notification published to {}", destination);
        } catch (Exception e) {
            log.warn("Failed to publish WebSocket notification for user {}: {}", recipientId, e.getMessage());
        }
    }
}
