package org.petproject.dating_backend.photo;


import org.petproject.dating_backend.common.exception.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

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

    @Query("SELECT p FROM PhotoEntity p WHERE p.isMain = true AND p.userId = :userId")
    Optional<PhotoEntity> findMainPhoto(Long userId);

    boolean existsByUserIdAndIsMainTrue(Long userId);

    @Modifying
    @Query("UPDATE PhotoEntity p SET p.isMain = false WHERE p.userId = :userId AND p.isMain = true")
    void clearMainFlag(Long userId);

}
