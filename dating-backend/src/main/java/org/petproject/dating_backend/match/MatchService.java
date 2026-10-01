package org.petproject.dating_backend.match;

import lombok.RequiredArgsConstructor;
import org.petproject.dating_backend.swipe.SwipeAction;
import org.petproject.dating_backend.swipe.SwipeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final SwipeRepository swipeRepository;

    @Transactional
    public boolean doMatch(Long fromUserId, Long toUserId, SwipeAction swipeAction) {
        boolean isMatched = false;

        if (swipeAction.equals(SwipeAction.LIKE)
                && swipeRepository.existsByFromIdAndToId(toUserId, fromUserId)) {

            MatchEntity matchEntity = new MatchEntity();
            matchEntity.setFirstUserId(Math.min(fromUserId, toUserId));
            matchEntity.setSecondUserId(Math.max(fromUserId, toUserId));
            matchRepository.save(matchEntity);

            isMatched = true;

        }

        return isMatched;
    }

}
