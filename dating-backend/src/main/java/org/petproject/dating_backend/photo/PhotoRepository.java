package org.petproject.dating_backend.photo;


import org.petproject.dating_backend.common.exception.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PhotoRepository extends JpaRepository<PhotoEntity, Long> {

    int countByUserId(Long userId);
    List<PhotoEntity> findByUserIdOrderByUploadedAtDesc(Long userId);

    default PhotoEntity findByIdOrThrow(Long id) throws NotFoundException {
        return findById(id).orElseThrow(
                () -> new NotFoundException("Фото не найдено", 404)
        );
    }

}
