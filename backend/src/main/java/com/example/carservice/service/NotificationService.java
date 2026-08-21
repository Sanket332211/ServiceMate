package com.example.carservice.service;

import com.example.carservice.dto.NotificationResponse;
import com.example.carservice.entity.Notification;
import com.example.carservice.entity.NotificationType;
import com.example.carservice.entity.User;
import com.example.carservice.exception.ResourceNotFoundException;
import com.example.carservice.repository.NotificationRepository;
import com.example.carservice.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * NotificationService
 *
 * Manages in-app notifications for both Customers and Service Center staff.
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates and persists an in-app notification.
     */
    @Transactional
    public NotificationResponse createNotification(User recipient, String title, String message,
                                                   NotificationType type, Long relatedBookingId) {
        Notification notification = new Notification(recipient, title, message, type, relatedBookingId);
        Notification saved = notificationRepository.save(notification);
        return NotificationResponse.fromEntity(saved);
    }

    /**
     * Returns all notifications for the authenticated user.
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Marks a notification as read.
     */
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        if (!notification.getRecipient().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to modify this notification.");
        }

        notification.setRead(true);
        Notification updated = notificationRepository.save(notification);
        return NotificationResponse.fromEntity(updated);
    }

    /**
     * Marks all unread notifications for the authenticated user as read.
     */
    @Transactional
    public void markAllAsRead(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        List<Notification> unreadList = notificationRepository.findByRecipientIdAndIsReadFalse(user.getId());
        for (Notification n : unreadList) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unreadList);
    }

    /**
     * Returns the count of unread notifications for a user.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        return notificationRepository.countByRecipientIdAndIsReadFalse(user.getId());
    }
}
