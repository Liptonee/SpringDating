package org.petproject.dating_backend.chat;

import org.petproject.dating_backend.match.MatchEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface ChatRoomRepository extends JpaRepository<ChatRoomEntity, Long> {

    @Query("""
            select count(r) > 0 from ChatRoomEntity r
            where r.id = :roomId
                and (r.firstUserId = :userId or r.secondUserId = :userId)
            """)
    boolean hasMemberById(@Param("roomId") Long roomId, @Param("userId") Long userId);


    /**
     * НЕ создаёт запись. Просто создаёт верный с точки зрения бд Entity.
     */
    default ChatRoomEntity createBetween(Long a, Long b) {
        ChatRoomEntity r = new ChatRoomEntity();
        r.setFirstUserId(Math.min(a, b));
        r.setSecondUserId(Math.max(a, b));
        r.setCreatedAt(Instant.now());
        return r;
    }

    @Query("""
            select r from ChatRoomEntity r
            where r.secondUserId = :userId or r.firstUserId = :userId
            order by r.createdAt desc
        """)
    Page<ChatRoomEntity> findAllByUserId(@Param("userId") Long userId, Pageable pageable);
}
