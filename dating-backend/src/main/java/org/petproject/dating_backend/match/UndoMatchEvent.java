package org.petproject.dating_backend.match;

public record UndoMatchEvent(
        Long firstUserId,

        Long secondUserId
) {
}
