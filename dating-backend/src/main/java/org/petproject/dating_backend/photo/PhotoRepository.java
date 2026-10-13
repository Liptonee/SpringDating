package org.petproject.dating_backend.photo;


import org.petproject.dating_backend.common.exception.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PhotoRepository extends JpaRepository<PhotoEntity, Long> {

    int countByUserId(Long userId);

    List<PhotoEntity> findByUserIdOrderByUploadedAtDesc(Long userId);

    default PhotoEntity findByIdOrThrow(Long id) throws NotFoundException {
        return findById(id).orElseThrow(
                () -> new NotFoundException("Фото не найдено", 404)
        );
    }

    @Query("""
            select case when count(p) > 0 then true else false end
            from PhotoEntity p
            where p.userId = :userId and p.isMain
""")
    boolean existsByUserIdAndIsMainTrue(@Param("userId") Long userId);

    @Modifying
    @Query("""
            UPDATE PhotoEntity p
            SET p.isMain = CASE WHEN p.id = :photoId THEN true ELSE false END
            WHERE p.userId = :userId
              AND (p.isMain = true OR p.id = :photoId)
            """)
    void setNewMainPhoto(@Param("userId") Long userId, @Param("photoId") Long photoId);

    @Query("SELECT p FROM PhotoEntity p WHERE p.isMain = true AND p.userId IN :userIds")
    List<PhotoEntity> findMainPhotosByUserIds(@Param("userIds") Collection<Long> userIds);

    @Query("""
            select p from PhotoEntity p
            where p.userId = :userId and p.isMain = true
""")
    Optional<PhotoEntity> findMain(@Param("userId") Long userId);


}
