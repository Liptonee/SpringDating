package org.petproject.dating_backend.swipe;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SwipeRepository extends JpaRepository<SwipeEntity, Long> {

    boolean existsByFromIdAndToId(Long fromId, Long toId);

}
