package org.petproject.dating_backend.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {


    @Query("""
            SELECT n FROM NotificationEntity n
            WHERE (n.userId = :curUserId)
                AND (:read IS NULL OR n.isRead = :read)
            ORDER BY n.createdAt DESC
            """)
    Page<NotificationEntity> findAllByUserId(Long curUserId, Boolean read, Pageable pageable);

}
