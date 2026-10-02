package org.petproject.dating_backend.match;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchRepository extends JpaRepository<MatchEntity, Long> {

    @Query("""
                SELECT COUNT(m) > 0 FROM MatchEntity m
                WHERE m.firstUserId = :first
                  AND m.secondUserId = :second
            """)
    boolean existsByPair(Long first, Long second);


    default boolean existsBetween(Long a, Long b) {
        long first = Math.min(a, b);
        long second = Math.max(a, b);
        return existsByPair(first, second);
    }

    /**
     * НЕ создаёт запись. Просто создаёт верный с точки зрения бд Entity.
     */
    default MatchEntity createBetween(Long a, Long b) {
        MatchEntity m = new MatchEntity();
        m.setFirstUserId(Math.min(a, b));
        m.setSecondUserId(Math.max(a, b));
        return m;
    }

    @Modifying
    @Query("""
                DELETE FROM MatchEntity m
                WHERE (m.firstUserId = :a AND m.secondUserId = :b)
                   OR (m.firstUserId = :b AND m.secondUserId = :a)
            """)
    int deleteBetween(@Param("a") Long a, @Param("b") Long b);

    @Query("""
                SELECT m FROM MatchEntity m
                WHERE m.firstUserId = :userId OR m.secondUserId = :userId
            """)
    Page<MatchEntity> findAllBySingleUserId(@Param("userId") Long userId, Pageable pageable);

}
