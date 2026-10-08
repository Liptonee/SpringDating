package org.petproject.dating_backend.match;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.petproject.dating_backend.common.exception.BadRequestException;
import org.petproject.dating_backend.swipe.SwipeAction;
import org.petproject.dating_backend.swipe.SwipeRepository;
import org.petproject.dating_backend.swipe.UndoLikeEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
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

    private final ApplicationEventPublisher eventPublisher;

    private final MeterRegistry registry;

    @Transactional(noRollbackFor = DataIntegrityViolationException.class)
    public boolean doMatch(Long fromUserId, Long toUserId, SwipeAction swipeAction) {
        if (fromUserId.equals(toUserId)) {
            throw new BadRequestException("Нельзя сделать мэтч с самим собой", 400);
        }

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
            registry.counter("matches.created.total").increment();

            eventPublisher.publishEvent(new MatchCreatedEvent(
                    matchEntity.getId(),
                    matchEntity.getFirstUserId(),
                    matchEntity.getSecondUserId(),
                    matchEntity.getCreatedAt()
            ));

        }


        return isMatched;
    }

    @Transactional(readOnly = true)
    public boolean existsBetween(Long fromId, Long toId) {
        return matchRepository.existsBetween(fromId, toId);
    }


    @Transactional(readOnly = true)
    //Недоработка - лучше бы возвращалось обогащённое dto 
    public Page<MatchDto> getMatches(Long curUserId, Pageable pageable) {

        return matchRepository.findAllBySingleUserId(curUserId, pageable).map(matchMapper::toDto);

    }

    @Transactional
    @EventListener
    public void undoMatch(UndoLikeEvent event) {
        int deleted = matchRepository.deleteBetween(event.fromUserId(), event.toUserId());
        registry.counter("matches.undone.total").increment();
        if (deleted > 0) {
            eventPublisher.publishEvent(new UndoMatchEvent(event.toUserId(), event.fromUserId()));
        }

    }

}
