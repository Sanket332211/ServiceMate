package com.example.carservice.controller;

import com.example.carservice.dto.NotificationResponse;
import com.example.carservice.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * NotificationController
 *
 * REST Controller for accessing and managing in-app notifications.
 */
@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(originPatterns = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS, RequestMethod.PATCH}, allowCredentials = "true")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * GET /api/notifications/my
     * Retrieves all notifications for the authenticated user.
     */
    @GetMapping("/my")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(Principal principal) {
        List<NotificationResponse> list = notificationService.getMyNotifications(principal.getName());
        return ResponseEntity.ok(list);
    }

    /**
     * PATCH /api/notifications/{id}/read
     * Marks a specific notification as read.
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long id, Principal principal) {
        NotificationResponse response = notificationService.markAsRead(id, principal.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /api/notifications/read-all
     * Marks all unread notifications for the authenticated user as read.
     */
    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(Principal principal) {
        notificationService.markAllAsRead(principal.getName());
        return ResponseEntity.ok(Map.of("success", true, "message", "All notifications marked as read."));
    }

    /**
     * GET /api/notifications/unread-count
     * Returns the count of unread notifications for badge indicators.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Principal principal) {
        long count = notificationService.getUnreadCount(principal.getName());
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }
}
