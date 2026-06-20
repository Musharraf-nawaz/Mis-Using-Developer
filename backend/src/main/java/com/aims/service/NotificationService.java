package com.aims.service;

import com.aims.dto.common.PageResponse;
import com.aims.dto.notification.NotificationResponse;
import com.aims.entity.Notification;
import com.aims.entity.User;
import com.aims.exception.ResourceNotFoundException;
import com.aims.repository.NotificationRepository;
import com.aims.repository.UserRepository;
import com.aims.util.MapperUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final SimpMessagingTemplate messagingTemplate;

    @Async
    @Transactional
    public void notify(Long userId, String title, String message, String type,
                       String entityType, Long entityId, boolean sendEmail) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .entityType(entityType)
                .entityId(entityId)
                .build();

        if (sendEmail) {
            try {
                SimpleMailMessage mail = new SimpleMailMessage();
                mail.setTo(user.getEmail());
                mail.setSubject(title);
                mail.setText(message);
                mailSender.send(mail);
                notification.setEmailSent(true);
            } catch (Exception e) {
                log.warn("Failed to send email notification: {}", e.getMessage());
            }
        }

        notification = notificationRepository.save(notification);
        try {
            messagingTemplate.convertAndSend("/topic/notifications/" + userId,
                    MapperUtils.toNotificationResponse(notification));
        } catch (Exception e) {
            log.warn("WebSocket notification push failed: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getUserNotifications(Long userId, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.from(page.map(MapperUtils::toNotificationResponse));
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long id, Long userId) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Notification not found");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }
}
