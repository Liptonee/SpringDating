package org.petproject.dating_backend.swipe;

public record UndoSwipeEvent (

        Long swipeId,

        Long fromUserId,

        Long toUserId

){
}
