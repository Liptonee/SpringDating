package org.petproject.dating_backend.swipe;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SwipeRepository extends JpaRepository<SwipeEntity, Long> {

    boolean existsByFromIdAndToId(Long fromId, Long toId);


    @Query("""
                SELECT s FROM SwipeEntity s
                WHERE (s.fromId = :userId)
                    AND (:action IS NULL OR s.action = :action)
                ORDER BY s.createdAt DESC
            """)
    Page<SwipeEntity> findAllForHistory(@Param("userId") Long userId,
                                        @Param("action") SwipeAction action,
                                        Pageable pageable);

    @Query("""
                SELECT s FROM SwipeEntity s
                WHERE s.toId = :userId AND s.action = SwipeAction.LIKE
                ORDER BY s.createdAt DESC
            """)
    Page<SwipeEntity> findAllForLiked(@Param("userId") Long userId,
                                      Pageable pageable);

}
