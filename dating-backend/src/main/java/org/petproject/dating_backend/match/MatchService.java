package org.petproject.dating_backend.match;

import lombok.RequiredArgsConstructor;
import org.petproject.dating_backend.swipe.SwipeAction;
import org.petproject.dating_backend.swipe.SwipeRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final SwipeRepository swipeRepository;
    private final MatchMapper matchMapper;

    @Transactional
    public boolean doMatch(Long fromUserId, Long toUserId, SwipeAction swipeAction) {
        boolean isMatched = false;

        if (swipeAction.equals(SwipeAction.LIKE)
                && swipeRepository.existsByFromIdAndToId(toUserId, fromUserId)) {

            MatchEntity matchEntity = matchRepository.createBetween(toUserId, fromUserId);
            try {
                matchRepository.saveAndFlush(matchEntity);
            } catch (DataIntegrityViolationException e) {
                return true;
            }

            isMatched = true;

        }

        return isMatched;
    }

    @Transactional(readOnly = true)
    public boolean existsBetween(Long fromId, Long toId) {
        return matchRepository.existsBetween(fromId, toId);
    }


    @Transactional
    public Page<MatchDto> getMatches(Long curUserId, Pageable pageable) {

        return matchRepository.findAllBySingleUserId(curUserId, pageable).map(matchMapper::toDto);

    }

    @Transactional
    public void undoMatch(Long fromId, Long toId) {
        matchRepository.deleteBetween(fromId, toId);
    }

}
