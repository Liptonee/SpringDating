package org.petproject.dating_backend.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {


    @Query("""
            SELECT n FROM NotificationEntity n
            WHERE (n.userId = :curUserId)
                AND (:read IS NULL OR n.isRead = :read)
            ORDER BY n.createdAt DESC
            """)
    Page<NotificationEntity> findAllByUserId(@Param("userId") Long userId, @Param("read") Boolean read, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE NotificationEntity n
        SET n.isRead = true
        WHERE n.userId = :userId
          AND n.isRead = false
    """)
    void markAllAsRead(@Param("userId") Long userId);

}
