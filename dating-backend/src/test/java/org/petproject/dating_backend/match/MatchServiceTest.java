package org.petproject.dating_backend.match;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.petproject.dating_backend.common.exception.BadRequestException;
import org.petproject.dating_backend.swipe.SwipeAction;
import org.petproject.dating_backend.swipe.SwipeRepository;
import org.petproject.dating_backend.swipe.UndoLikeEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private MatchRepository matchRepository;
    @Mock
    private SwipeRepository swipeRepository;
    @Mock
    private MatchMapper matchMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private MeterRegistry registry;
    @Mock
    private Counter counter;

    @Captor
    ArgumentCaptor<MatchEntity> matchEntityCaptor;
    @Captor
    ArgumentCaptor<MatchCreatedEvent> createdEventCaptor;
    @Captor
    ArgumentCaptor<UndoMatchEvent> undoMatchEventCaptor;

    private MatchService matchService;

    private static final Long FROM_USER_ID = 123L;
    private static final Long TO_USER_ID = 321L;

    @BeforeEach
    void setUp() {
        matchService = new MatchService(
                matchRepository,
                swipeRepository,
                matchMapper,
                eventPublisher,
                registry
        );
        lenient().when(registry.counter(any())).thenReturn(counter);
    }

    @Nested
    class DoMatchTest {

        @Test
        void throwsBadRequest_whenIdsEquals() {
            Long fromUserId = 123L;
            Long toUserId = 123L;
            SwipeAction action = mock(SwipeAction.class);

            assertThatThrownBy(() -> matchService.doMatch(fromUserId, toUserId, action))
                    .isExactlyInstanceOf(BadRequestException.class)
                    .hasMessage("Нельзя сделать мэтч с самим собой");

            verify(matchRepository, never()).saveAndFlush(any(MatchEntity.class));
            verifyNoInteractions(swipeRepository, eventPublisher, registry);
        }

        @Test
        void returnsFalseAndDoNothing_whenDislike() {
            SwipeAction action = SwipeAction.DISLIKE;

            boolean isMatched = matchService.doMatch(FROM_USER_ID, TO_USER_ID, action);

            assertThat(isMatched).isFalse();
            verify(swipeRepository, never()).existsByFromIdAndToId(any(Long.class), any(Long.class));
            verify(matchRepository, never()).createBetween(any(Long.class), any(Long.class));
            verify(matchRepository, never()).saveAndFlush(any(MatchEntity.class));
            verify(eventPublisher, never()).publishEvent(any(MatchCreatedEvent.class));
        }

        @Test
        void returnsFalseAndDoNothing_whenLikeNotMutual() {
            SwipeAction action = SwipeAction.LIKE;
            when(swipeRepository.existsByFromIdAndToId(TO_USER_ID, FROM_USER_ID)).thenReturn(false);

            boolean isMatched = matchService.doMatch(FROM_USER_ID, TO_USER_ID, action);

            assertThat(isMatched).isFalse();
            verify(swipeRepository).existsByFromIdAndToId(TO_USER_ID, FROM_USER_ID);
            verify(matchRepository, never()).createBetween(any(Long.class), any(Long.class));
            verify(matchRepository, never()).saveAndFlush(any(MatchEntity.class));
            verify(eventPublisher, never()).publishEvent(any(MatchCreatedEvent.class));
        }

        @Test
        void returnsTrue_whenParallelSave() {
            SwipeAction action = SwipeAction.LIKE;
            when(swipeRepository.existsByFromIdAndToId(TO_USER_ID, FROM_USER_ID)).thenReturn(true);
            when(matchRepository.createBetween(TO_USER_ID, FROM_USER_ID)).thenReturn(mock(MatchEntity.class));
            when(matchRepository.saveAndFlush(any(MatchEntity.class))).thenThrow(DataIntegrityViolationException.class);

            boolean isMatched = matchService.doMatch(FROM_USER_ID, TO_USER_ID, action);

            assertThat(isMatched).isTrue();
            verify(eventPublisher, never()).publishEvent(any(MatchCreatedEvent.class));
            verify(swipeRepository).existsByFromIdAndToId(TO_USER_ID, FROM_USER_ID);
            verify(matchRepository).createBetween(any(Long.class), any(Long.class));
            verify(matchRepository).saveAndFlush(any(MatchEntity.class));
        }

        @Test
        void returnsTrue_whenHappyPath() {
            MatchEntity entity = new MatchEntity();
            entity.setId(10L);
            entity.setFirstUserId(FROM_USER_ID);
            entity.setSecondUserId(TO_USER_ID);

            SwipeAction action = SwipeAction.LIKE;
            when(swipeRepository.existsByFromIdAndToId(TO_USER_ID, FROM_USER_ID)).thenReturn(true);
            when(matchRepository.createBetween(TO_USER_ID, FROM_USER_ID)).thenReturn(entity);
            when(matchRepository.saveAndFlush(any(MatchEntity.class))).thenReturn(entity);

            boolean isMatched = matchService.doMatch(FROM_USER_ID, TO_USER_ID, action);

            assertThat(isMatched).isTrue();

            verify(eventPublisher).publishEvent(createdEventCaptor.capture());
            MatchCreatedEvent actualEvent = createdEventCaptor.getValue();
            assertThat(actualEvent)
                    .extracting(
                            MatchCreatedEvent::matchId,
                            MatchCreatedEvent::firstUserId,
                            MatchCreatedEvent::secondUserId
                    )
                    .containsExactly(10L, FROM_USER_ID, TO_USER_ID);

            verify(swipeRepository).existsByFromIdAndToId(TO_USER_ID, FROM_USER_ID);
            verify(matchRepository).createBetween(any(Long.class), any(Long.class));

            verify(matchRepository).saveAndFlush(matchEntityCaptor.capture());
            MatchEntity actualEntity = matchEntityCaptor.getValue();
            assertThat(actualEntity)
                    .extracting(
                            MatchEntity::getId,
                            MatchEntity::getFirstUserId,
                            MatchEntity::getSecondUserId
                    )
                    .containsExactly(10L, FROM_USER_ID, TO_USER_ID);

            verify(registry).counter("matches.created.total");
            verify(counter).increment();
        }
    }

    @Nested
    class ExistsBetweenTest {

        @Test
        void returnsRepositoryResult() {
            when(matchRepository.existsBetween(FROM_USER_ID, TO_USER_ID)).thenReturn(true);

            boolean exists = matchService.existsBetween(FROM_USER_ID, TO_USER_ID);

            assertThat(exists).isTrue();
            verify(matchRepository).existsBetween(FROM_USER_ID, TO_USER_ID);
            verifyNoInteractions(swipeRepository, matchMapper, eventPublisher);
        }
    }

    @Nested
    class GetMatchesTest {

        @Test
        void returnsMappedPage() {
            MatchEntity entity = new MatchEntity();
            entity.setId(10L);
            entity.setFirstUserId(FROM_USER_ID);
            entity.setSecondUserId(TO_USER_ID);

            Pageable pageable = PageRequest.of(0, 20);
            Page<MatchEntity> page = new PageImpl<>(List.of(entity), pageable, 1);
            MatchDto dto = new MatchDto(10L, FROM_USER_ID, TO_USER_ID, entity.getCreatedAt());

            when(matchRepository.findAllBySingleUserId(FROM_USER_ID, pageable)).thenReturn(page);
            when(matchMapper.toDto(entity)).thenReturn(dto);

            Page<MatchDto> result = matchService.getMatches(FROM_USER_ID, pageable);

            assertThat(result.getContent()).containsExactly(dto);
            assertThat(result.getTotalElements()).isEqualTo(1);

            verify(matchRepository).findAllBySingleUserId(FROM_USER_ID, pageable);
            verify(matchMapper).toDto(entity);
            verifyNoInteractions(swipeRepository, eventPublisher);
        }
    }

    @Nested
    class UndoMatchTest {

        @Test
        void publishesUndoEvent_whenMatchDeleted() {
            UndoLikeEvent event = new UndoLikeEvent(1L, FROM_USER_ID, TO_USER_ID);
            when(matchRepository.deleteBetween(FROM_USER_ID, TO_USER_ID)).thenReturn(1);

            matchService.undoMatch(event);

            verify(matchRepository).deleteBetween(FROM_USER_ID, TO_USER_ID);
            verify(registry).counter("matches.undone.total");
            verify(counter).increment();

            verify(eventPublisher).publishEvent(undoMatchEventCaptor.capture());
            UndoMatchEvent actual = undoMatchEventCaptor.getValue();
            assertThat(actual)
                    .extracting(UndoMatchEvent::firstUserId, UndoMatchEvent::secondUserId)
                    .containsExactly(TO_USER_ID, FROM_USER_ID);

            verifyNoInteractions(swipeRepository, matchMapper);
        }

        @Test
        void doesNotPublishUndoEvent_whenNothingDeleted() {
            UndoLikeEvent event = new UndoLikeEvent(1L, FROM_USER_ID, TO_USER_ID);
            when(matchRepository.deleteBetween(FROM_USER_ID, TO_USER_ID)).thenReturn(0);

            matchService.undoMatch(event);

            verify(matchRepository).deleteBetween(FROM_USER_ID, TO_USER_ID);
            verify(registry).counter("matches.undone.total");
            verify(counter).increment();
            verify(eventPublisher, never()).publishEvent(any(UndoMatchEvent.class));
            verifyNoInteractions(swipeRepository, matchMapper);
        }
    }
}