package org.petproject.dating_backend.deck;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.petproject.dating_backend.photo.PhotoService;
import org.petproject.dating_backend.user.GetUserDto;
import org.petproject.dating_backend.user.UserGender;
import org.petproject.dating_backend.user.UserService;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeckServiceTest {

    @Mock
    private PhotoService photoService;
    @Mock
    private UserService userService;
    @Mock
    private MeterRegistry registry;
    @Mock
    private Counter counter;

    private DeckService deckService;

    private static final Long CUR_USER_ID = 1L;
    private static final Short QUANTITY = 10;

    @BeforeEach
    void setUp() {
        deckService = new DeckService(photoService, userService, registry);
        lenient().when(registry.counter(anyString())).thenReturn(counter);
    }

    private static GetUserDto user(
            Long id,
            String firstName,
            String shortAbout,
            String fullAbout,
            Short age,
            String city,
            UserGender gender
    ) {
        return new GetUserDto(id, firstName, shortAbout, fullAbout, age, city, gender);
    }

    @Nested
    class GetDeck {

        @Test
        void returnsEmptyList_andIncrementsCounters_whenNoEligibleUsers() {
            when(userService.getByPreferences(CUR_USER_ID, QUANTITY)).thenReturn(List.of());

            List<CardDto> result = deckService.getDeck(CUR_USER_ID, QUANTITY);

            assertThat(result).isEmpty();
            verify(registry).counter("deck.requests.total");
            verify(registry).counter("deck.empty.total");
            verify(counter, times(2)).increment();
            verifyNoInteractions(photoService);
        }


        @ParameterizedTest
        @MethodSource("provideDeckScenarios")
        void returnsCorrectCards_whenEligibleUsersExist(
                List<GetUserDto> eligibleUsers,
                Map<Long, String> mainPhotoUrls,
                List<CardDto> expectedCards
        ) {
            when(userService.getByPreferences(CUR_USER_ID, QUANTITY)).thenReturn(eligibleUsers);
            when(photoService.getMainPhotoUrls(anyList())).thenReturn(mainPhotoUrls);

            List<CardDto> result = deckService.getDeck(CUR_USER_ID, QUANTITY);

            assertThat(result).containsExactlyElementsOf(expectedCards);

            List<Long> expectedIds = eligibleUsers.stream().map(GetUserDto::id).toList();
            verify(photoService).getMainPhotoUrls(expectedIds);

            verify(registry).counter("deck.requests.total");
            verify(registry, never()).counter("deck.empty.total");
            verify(counter).increment();
        }

        private static Stream<Arguments> provideDeckScenarios() {
            GetUserDto user1 = user(1L, "Иван", "Кратко", "Полно о Иване",
                    (short) 25, "Москва", UserGender.MALE);
            GetUserDto user2 = user(2L, "Петр", "О себе", "Полно о Петре",
                    (short) 30, "Санкт-Петербург", UserGender.MALE);
            GetUserDto user3 = user(3L, "Анна", "Люблю котиков", "Полно об Анне",
                    (short) 22, "Казань", UserGender.FEMALE);

            return Stream.of(
                    Arguments.of(
                            List.of(user1),
                            Map.of(1L, "http://photo1"),
                            List.of(new CardDto(1L, "http://photo1", "Иван", "Кратко", (short) 25))
                    ),
                    Arguments.of(
                            List.of(user2),
                            Map.of(),
                            List.of(new CardDto(2L, null, "Петр", "О себе", (short) 30))
                    ),
                    Arguments.of(
                            List.of(user1, user2, user3),
                            Map.of(1L, "url1", 3L, "url3"),
                            List.of(
                                    new CardDto(1L, "url1", "Иван", "Кратко", (short) 25),
                                    new CardDto(2L, null, "Петр", "О себе", (short) 30),
                                    new CardDto(3L, "url3", "Анна", "Люблю котиков", (short) 22)
                            )
                    )
            );
        }

    }

}