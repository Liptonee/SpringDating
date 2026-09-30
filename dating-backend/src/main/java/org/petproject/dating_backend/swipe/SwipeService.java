package org.petproject.dating_backend.swipe;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SwipeService {

    private final SwipeRepository swipeRepository;

    @Transactional
    public void swipe(Long curUserId, SwipeDto swipeDto){
        SwipeEntity swipeEntity = new SwipeEntity();
        swipeEntity.setFromId(curUserId);
        swipeEntity.setToId(swipeDto.toId());
        swipeEntity.setAction(swipeDto.action());

        //todo matchService and chatService and notificationService
        swipeRepository.save(swipeEntity);

    }


}
