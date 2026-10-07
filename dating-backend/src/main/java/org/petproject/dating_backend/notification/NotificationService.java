package org.petproject.dating_backend.notification;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.petproject.dating_backend.common.exception.ForbiddenException;
import org.petproject.dating_backend.common.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {


    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    private final MeterRegistry registry;

    @Transactional
    public void create(NotificationRequestDto requestDto){
        NotificationEntity entity = new NotificationEntity();
        entity.setUserId(requestDto.userId());
        entity.setTitle(requestDto.title());
        entity.setBody(requestDto.body());
        entity.setType(requestDto.type());

        notificationRepository.save(entity);
        registry.counter("notifications.created.total",
                "type", requestDto.type().name()).increment();
    }


    @Transactional
    public void read(Long curUserId, Long notifId) {
        NotificationEntity entity = notificationRepository.findById(notifId).orElseThrow(
                () -> new NotFoundException("Уведомления с таким id не существует", 404)
        );
        if (!entity.getUserId().equals(curUserId)) {
            throw new ForbiddenException("Вы не можете прочитать чужое уведомление", 403);
        }

        entity.setIsRead(true);
        notificationRepository.save(entity);
        registry.counter("notifications.read.total").increment();
    }

    @Transactional(readOnly = true)
    public NotificationResponseDto getSingleNotification(Long curUserId, Long notifId) {
        NotificationEntity entity = notificationRepository.findById(notifId).orElseThrow(
                () -> new NotFoundException("Уведомления с таким id не существует", 404)
        );

        if (!entity.getUserId().equals(curUserId)) {
            throw new ForbiddenException("Вы не можете просмотреть чужое уведомление", 403);
        }

        registry.counter("notifications.read_all.total").increment();
        return notificationMapper.toResponse(entity);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getNotifications(Long curUserId, Boolean read, Pageable pageable) {

        return notificationRepository.findAllByUserId(curUserId, read, pageable)
                .map(notificationMapper::toResponse);

    }

    @Transactional
    public void markAllAsRead(Long curUserId) {
        notificationRepository.markAllAsRead(curUserId);
    }

}
