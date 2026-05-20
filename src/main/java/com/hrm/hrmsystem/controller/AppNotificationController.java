package com.hrm.hrmsystem.controller;

import com.hrm.hrmsystem.model.AppNotification;
import com.hrm.hrmsystem.model.User;
import com.hrm.hrmsystem.repository.AppNotificationRepository;
import com.hrm.hrmsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class AppNotificationController {

    private final AppNotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> getUserNotifications(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<AppNotification> notifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(user.getId());
        long unreadCount = notificationRepository.countByRecipientIdAndIsReadFalse(user.getId());

        return ResponseEntity.ok(Map.of(
                "notifications", notifications,
                "unreadCount", unreadCount
        ));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id, Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        AppNotification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getRecipient().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Not authorized to read this notification");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
        return ResponseEntity.ok(Map.of("message", "Marked as read"));
    }

    @PostMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<AppNotification> unread = notificationRepository.findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(user.getId());
        for (AppNotification notification : unread) {
            notification.setRead(true);
        }
        notificationRepository.saveAll(unread);
        return ResponseEntity.ok(Map.of("message", "All marked as read"));
    }
}
