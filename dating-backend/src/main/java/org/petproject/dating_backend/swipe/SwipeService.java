package org.petproject.dating_backend.swipe;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.petproject.dating_backend.common.exception.BadRequestException;
import org.petproject.dating_backend.common.exception.NotFoundException;
import org.petproject.dating_backend.match.MatchService;
import org.petproject.dating_backend.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SwipeService {

    private final SwipeRepository swipeRepository;
    private final UserRepository userRepository;
    private final MatchService matchService;

    @Transactional
    public SwipeResponseDto swipe(Long curUserId, SwipeDto swipeDto) {
        if (curUserId.equals(swipeDto.toId())) {
            throw new BadRequestException("Нельзя свайпам самого себя", 400);
        }
        if (swipeRepository.existsByFromIdAndToId(curUserId, swipeDto.toId())) {
            log.debug("Duplicate swipe from {} to {}", curUserId, swipeDto.toId());
            return new SwipeResponseDto(false);
        }
        if (!userRepository.existsById(swipeDto.toId())) {
            throw new NotFoundException("Пользователь не найден", 404);
        }

        SwipeEntity swipeEntity = new SwipeEntity();
        swipeEntity.setFromId(curUserId);
        swipeEntity.setToId(swipeDto.toId());
        swipeEntity.setAction(swipeDto.action());

        try {
            swipeRepository.saveAndFlush(swipeEntity);
        } catch (DataIntegrityViolationException e) {
            return new SwipeResponseDto(false);
        }

        boolean isMatched = matchService.doMatch(curUserId, swipeDto.toId(), swipeDto.action());

        return new SwipeResponseDto(isMatched);


    }


}
