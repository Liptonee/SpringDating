package org.petproject.dating_backend.swipe;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.petproject.dating_backend.common.exception.BadRequestException;
import org.petproject.dating_backend.common.exception.ForbiddenException;
import org.petproject.dating_backend.common.exception.NotFoundException;
import org.petproject.dating_backend.match.MatchService;
import org.petproject.dating_backend.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
public class SwipeService {


    private final SwipeRepository swipeRepository;
    private final UserRepository userRepository;
    private final MatchService matchService;
    private final SwipeMapper swipeMapper;

    @Transactional
    public SwipeResponseDto swipe(Long curUserId, SwipeRequestDto swipeRequestDto) {
        if (curUserId.equals(swipeRequestDto.toId())) {
            throw new BadRequestException("Нельзя свайпам самого себя", 400);
        }
        if (!userRepository.existsById(swipeRequestDto.toId())) {
            throw new NotFoundException("Пользователь не найден", 404);
        }
        if (swipeRepository.existsByFromIdAndToId(curUserId, swipeRequestDto.toId())) {
            log.debug("Duplicate swipe from {} to {}", curUserId, swipeRequestDto.toId());
            boolean alreadyMatched = matchService.existsBetween(curUserId, swipeRequestDto.toId());
            return new SwipeResponseDto(alreadyMatched);
        }

        SwipeEntity swipeEntity = new SwipeEntity();
        swipeEntity.setFromId(curUserId);
        swipeEntity.setToId(swipeRequestDto.toId());
        swipeEntity.setAction(swipeRequestDto.action());

        try {
            swipeRepository.saveAndFlush(swipeEntity);
        } catch (DataIntegrityViolationException e) {
            log.debug("Duplicate swipe from {} to {}", curUserId, swipeRequestDto.toId());
            boolean alreadyMatched = matchService.existsBetween(curUserId, swipeRequestDto.toId());
            return new SwipeResponseDto(alreadyMatched);
        }

        boolean isMatched = matchService.doMatch(curUserId, swipeRequestDto.toId(), swipeRequestDto.action());

        return new SwipeResponseDto(isMatched);

    }

    @Transactional(readOnly = true)
    public Page<SwipeHistoryDto> getHistory(Long curUserId, SwipeAction action, Pageable pageable) {
        return swipeRepository.findAllForHistory(curUserId, action, pageable).map(swipeMapper::toHistoryDto);
    }

    @Transactional(readOnly = true)
    public Page<SwipeHistoryDto> getLiked(Long curUserId, Pageable pageable) {

        return swipeRepository.findAllForLiked(curUserId, pageable).map(swipeMapper::toHistoryDto);

    }


    @Transactional
    public void undoSwipe(Long curUserId, Long swipeId) {
        SwipeEntity swipeEntity = swipeRepository.findById(swipeId).orElseThrow(
                () -> new NotFoundException("Свайпа с таким id не существует", 404)
        );
        if (!swipeEntity.getFromId().equals(curUserId)) {
            throw new ForbiddenException("Вы не можете отменить чужой свайп", 403);
        }

        swipeRepository.delete(swipeEntity);
        matchService.undoMatch(swipeEntity.getFromId(), swipeEntity.getToId());

    }
}
