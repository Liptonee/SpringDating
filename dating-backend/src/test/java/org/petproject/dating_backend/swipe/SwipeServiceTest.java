package org.petproject.dating_backend.swipe;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.petproject.dating_backend.common.exception.BadRequestException;
import org.petproject.dating_backend.common.exception.ForbiddenException;
import org.petproject.dating_backend.common.exception.NotFoundException;
import org.petproject.dating_backend.match.MatchService;
import org.petproject.dating_backend.user.UserRepository;
import org.petproject.dating_backend.user.UserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SwipeServiceTest {

    private static final Long USER_ID = 123L;
    private static final Long TO_ID = 456L;
    private static final Long SWIPE_ID = 789L;

    @Mock
    private SwipeRepository swipeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;
    @Mock
    private MatchService matchService;
    @Mock
    private SwipeMapper swipeMapper;
    @Mock
    private MeterRegistry registry;
    @Mock
    private Counter counter;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SwipeService swipeService;

    @BeforeEach
    void setUp() {
        lenient().when(registry.counter(anyString())).thenReturn(counter);
        lenient().when(registry.counter(anyString(), any(String[].class))).thenReturn(counter);
    }

    private static SwipeEntity swipe(Long fromId, Long toId, SwipeAction action) {
        SwipeEntity s = new SwipeEntity();
        s.setId(SWIPE_ID);
        s.setFromId(fromId);
        s.setToId(toId);
        s.setAction(action);
        s.setCreatedAt(Instant.now());
        return s;
    }

    private static SwipeRequestDto request(Long toId, SwipeAction action) {
        return new SwipeRequestDto(toId, action);
    }

    @Nested
    class SwipeTest {

        @Test
        void throwsBadRequest_whenSwipingSelf() {
            SwipeRequestDto request = request(USER_ID, SwipeAction.LIKE);
            assertThatThrownBy(() -> swipeService.swipe(USER_ID, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Нельзя свайпам самого себя");

            verifyNoInteractions(userRepository, swipeRepository, matchService, eventPublisher);
        }

        @Test
        void throwsNotFound_whenTargetUserDoesNotExist() {
            SwipeRequestDto request = request(TO_ID, SwipeAction.LIKE);
            when(userRepository.existsById(TO_ID)).thenReturn(false);

            assertThatThrownBy(() -> swipeService.swipe(USER_ID, request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Пользователь не найден");

            verify(swipeRepository, never()).saveAndFlush(any());
        }

        @Test
        void throwsForbidden_whenProfileNotReady() {
            SwipeRequestDto request = request(TO_ID, SwipeAction.LIKE);
            when(userRepository.existsById(TO_ID)).thenReturn(true);
            when(userService.getNotReadyFields(USER_ID)).thenReturn(Set.of("age"));

            assertThatThrownBy(() -> swipeService.swipe(USER_ID, request))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Пользователь не может свайпать по не заполнит профиль");

            verify(swipeRepository, never()).saveAndFlush(any());
        }

        @Test
        void returnsAlreadyMatched_whenDuplicateSwipeExists() {
            when(userRepository.existsById(TO_ID)).thenReturn(true);
            when(userService.getNotReadyFields(USER_ID)).thenReturn(Set.of());
            when(swipeRepository.existsByFromIdAndToId(USER_ID, TO_ID)).thenReturn(true);
            when(matchService.existsBetween(USER_ID, TO_ID)).thenReturn(true);

            SwipeResponseDto result = swipeService.swipe(USER_ID, request(TO_ID, SwipeAction.LIKE));

            assertThat(result.isMatched()).isTrue();
            verify(swipeRepository, never()).saveAndFlush(any());
            verifyNoInteractions(eventPublisher);
        }

        @Test
        void returnsAlreadyMatched_whenDataIntegrityViolationOnSave() {
            when(userRepository.existsById(TO_ID)).thenReturn(true);
            when(userService.getNotReadyFields(USER_ID)).thenReturn(Set.of());
            when(swipeRepository.existsByFromIdAndToId(USER_ID, TO_ID)).thenReturn(false);
            when(swipeRepository.saveAndFlush(any())).thenThrow(DataIntegrityViolationException.class);
            when(matchService.existsBetween(USER_ID, TO_ID)).thenReturn(true);

            SwipeResponseDto result = swipeService.swipe(USER_ID, request(TO_ID, SwipeAction.LIKE));

            assertThat(result.isMatched()).isTrue();
            verifyNoInteractions(eventPublisher);
        }

        @Test
        void savesSwipeAndPublishesLikeEvent_whenLike() {
            when(userRepository.existsById(TO_ID)).thenReturn(true);
            when(userService.getNotReadyFields(USER_ID)).thenReturn(Set.of());
            when(swipeRepository.existsByFromIdAndToId(USER_ID, TO_ID)).thenReturn(false);
            when(swipeRepository.saveAndFlush(any())).thenAnswer(inv -> {
                SwipeEntity e = inv.getArgument(0);
                e.setId(SWIPE_ID);
                return e;
            });
            when(matchService.doMatch(USER_ID, TO_ID, SwipeAction.LIKE)).thenReturn(true);

            SwipeResponseDto result = swipeService.swipe(USER_ID, request(TO_ID, SwipeAction.LIKE));

            assertThat(result.isMatched()).isTrue();
            verify(swipeRepository).saveAndFlush(any(SwipeEntity.class));
            verify(eventPublisher).publishEvent(any(LikeEvent.class));
            verify(counter).increment();
        }

        @Test
        void doesNotPublishLikeEvent_whenDislike() {
            when(userRepository.existsById(TO_ID)).thenReturn(true);
            when(userService.getNotReadyFields(USER_ID)).thenReturn(Set.of());
            when(swipeRepository.existsByFromIdAndToId(USER_ID, TO_ID)).thenReturn(false);
            when(swipeRepository.saveAndFlush(any())).thenAnswer(inv -> {
                SwipeEntity e = inv.getArgument(0);
                e.setId(SWIPE_ID);
                return e;
            });
            when(matchService.doMatch(USER_ID, TO_ID, SwipeAction.DISLIKE)).thenReturn(false);

            SwipeResponseDto result = swipeService.swipe(USER_ID, request(TO_ID, SwipeAction.DISLIKE));

            assertThat(result.isMatched()).isFalse();
            verify(swipeRepository).saveAndFlush(any(SwipeEntity.class));
            verify(eventPublisher, never()).publishEvent(any(LikeEvent.class));
            verify(counter).increment();
        }
    }

    @Nested
    class GetHistoryTest {

        @Test
        void returnsMappedPage() {
            Pageable pageable = PageRequest.of(0, 10);
            SwipeEntity entity = swipe(USER_ID, TO_ID, SwipeAction.LIKE);
            SwipeHistoryDto dto = new SwipeHistoryDto(TO_ID, SwipeAction.LIKE, entity.getCreatedAt());
            Page<SwipeEntity> page = new PageImpl<>(List.of(entity));

            when(swipeRepository.findAllForHistory(USER_ID, SwipeAction.LIKE, pageable)).thenReturn(page);
            when(swipeMapper.toHistoryDto(entity)).thenReturn(dto);

            Page<SwipeHistoryDto> result = swipeService.getHistory(USER_ID, SwipeAction.LIKE, pageable);

            assertThat(result.getContent()).containsExactly(dto);
            verify(swipeRepository).findAllForHistory(USER_ID, SwipeAction.LIKE, pageable);
            verify(swipeMapper).toHistoryDto(entity);
        }
    }

    @Nested
    class GetLikedTest {

        @Test
        void returnsMappedPage() {
            Pageable pageable = PageRequest.of(0, 10);
            SwipeEntity entity = swipe(TO_ID, USER_ID, SwipeAction.LIKE);
            SwipeLikedDto dto = new SwipeLikedDto(TO_ID, entity.getCreatedAt());
            Page<SwipeEntity> page = new PageImpl<>(List.of(entity));

            when(swipeRepository.findAllForLiked(USER_ID, pageable)).thenReturn(page);
            when(swipeMapper.toLikedDto(entity)).thenReturn(dto);

            Page<SwipeLikedDto> result = swipeService.getLiked(USER_ID, pageable);

            assertThat(result.getContent()).containsExactly(dto);
            verify(swipeRepository).findAllForLiked(USER_ID, pageable);
            verify(swipeMapper).toLikedDto(entity);
        }
    }

    @Nested
    class UndoSwipeTest {

        @Test
        void throwsNotFound_whenSwipeDoesNotExist() {
            when(swipeRepository.findById(SWIPE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> swipeService.undoSwipe(USER_ID, SWIPE_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Свайпа с таким id не существует");

            verify(swipeRepository, never()).delete(any());
        }

        @Test
        void throwsForbidden_whenSwipeBelongsToAnotherUser() {
            SwipeEntity entity = swipe(TO_ID, USER_ID, SwipeAction.LIKE);
            when(swipeRepository.findById(SWIPE_ID)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> swipeService.undoSwipe(USER_ID, SWIPE_ID))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Вы не можете отменить чужой свайп");

            verify(swipeRepository, never()).delete(any());
        }

        @Test
        void deletesSwipeAndPublishesUndoLikeEvent_whenLike() {
            SwipeEntity entity = swipe(USER_ID, TO_ID, SwipeAction.LIKE);
            when(swipeRepository.findById(SWIPE_ID)).thenReturn(Optional.of(entity));

            swipeService.undoSwipe(USER_ID, SWIPE_ID);

            verify(swipeRepository).delete(entity);
            verify(eventPublisher).publishEvent(any(UndoLikeEvent.class));
            verify(counter).increment();
        }

        @Test
        void doesNotPublishUndoLikeEvent_whenDislike() {
            SwipeEntity entity = swipe(USER_ID, TO_ID, SwipeAction.DISLIKE);
            when(swipeRepository.findById(SWIPE_ID)).thenReturn(Optional.of(entity));

            swipeService.undoSwipe(USER_ID, SWIPE_ID);

            verify(swipeRepository).delete(entity);
            verify(eventPublisher, never()).publishEvent(any(UndoLikeEvent.class));
            verify(counter).increment();
        }
    }
}