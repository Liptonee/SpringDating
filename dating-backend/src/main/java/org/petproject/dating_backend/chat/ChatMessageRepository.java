package org.petproject.dating_backend.chat;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    @Modifying
    @Query("""
                        update ChatMessageEntity m
                        set m.isRead = true
                        where m.roomId = :roomId
                            and m.senderId != :readerId
                            and m.isRead = false
            """)
    void markAllReadInRoom(@Param("roomId") Long roomId, @Param("readerId") Long readerId);

    @Query("""
                    select m from ChatMessageEntity m
                    where m.roomId = :roomId
                        and (:beforeId is null or m.id < :beforeId)
                    order by  m.id desc
            """)
    List<ChatMessageEntity> findPage(@Param("roomId") Long roomId,
                                     @Param("beforeId") Long beforeId,
                                     Pageable pageable);


    long countByRoomId(Long roomId);

}
