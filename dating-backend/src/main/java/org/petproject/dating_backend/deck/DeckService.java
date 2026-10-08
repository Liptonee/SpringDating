package org.petproject.dating_backend.deck;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.petproject.dating_backend.photo.PhotoService;
import org.petproject.dating_backend.user.GetUserDto;
import org.petproject.dating_backend.user.UserService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeckService {

    private final PhotoService photoService;
    private final UserService userService;

    private final MeterRegistry registry;

    public List<CardDto> getDeck(Long curUserId, Short quantity) {
        List<GetUserDto> eligibleUsers = userService.getByPreferences(curUserId, quantity);
        if (eligibleUsers.isEmpty()) {
            registry.counter("deck.requests.total").increment();
            registry.counter("deck.empty.total").increment();
            return List.of();
        }

        List<Long> ids = eligibleUsers.stream().map(GetUserDto::id).toList();
        Map<Long, String> mainPhotoUrls = photoService.getMainPhotoUrls(ids);

        List<CardDto> cards = eligibleUsers.stream()
                .map(u -> new CardDto(
                        u.id(),
                        mainPhotoUrls.get(u.id()),
                        u.firstName(),
                        u.shortAbout(),
                        u.age()
                ))
                .toList();

        registry.counter("deck.requests.total").increment();
        return cards;
    }


}
