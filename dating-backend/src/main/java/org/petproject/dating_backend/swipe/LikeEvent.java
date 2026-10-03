package org.petproject.dating_backend.swipe;

import java.time.Instant;

public record LikeEvent(

        Long swipeId,

        Long fromId,

        Long toId,

        Instant created_at

) {
}
