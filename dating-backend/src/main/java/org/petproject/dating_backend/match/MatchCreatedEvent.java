package org.petproject.dating_backend.match;

import java.time.Instant;

public record MatchCreatedEvent(

        Long matchId,

        Long firstUserId,

        Long secondUserId,

        Instant created_at
) {
}
