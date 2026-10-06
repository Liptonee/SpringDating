package org.petproject.dating_backend.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoomEntity, Long> {

    @Query("""
            select count(r) > 0 from ChatRoomEntity r
            where r.id = :roomId
                and r.firstUserId = :userId or r.secondUserId = :userId
            """)
    boolean hasMemberById(@Param("roomId") Long roomId, @Param("userId") Long userId);

}
