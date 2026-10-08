package org.petproject.dating_backend.swipe;

public record UndoLikeEvent(

        Long swipeId,

        Long fromUserId,

        Long toUserId

){
}
