package org.petproject.dating_backend.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {


    private final NotificationRepository notificationRepository;

    @Transactional
    public void create(NotificationRequestDto requestDto){
        NotificationEntity entity = new NotificationEntity();
        entity.setUserId(requestDto.userId());
        entity.setTitle(requestDto.title());
        entity.setBody(requestDto.body());
        entity.setType(requestDto.type());

        notificationRepository.save(entity);
    }


}
