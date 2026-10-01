package org.petproject.dating_backend.deck;

import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.petproject.dating_backend.photo.PhotoService;
import org.petproject.dating_backend.user.GetUserDto;
import org.petproject.dating_backend.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeckService {

    private final PhotoService photoService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<CardDto> getDeck(Long curUserId, Short quantity) throws MinioException {
        List<GetUserDto> eligibleUsers = userService.getByPreferences(curUserId, quantity);
        List<CardDto> cards = new ArrayList<>();

        for (GetUserDto user : eligibleUsers) {
            String url = null;
            try { url = photoService.getMainPhotoUrl(user.id()); }
            catch (Exception e) { log.warn("No main photo for {}", user.id()); }
            cards.add(new CardDto(
                    user.id(),
                    url,
                    user.firstName(),
                    user.shortAbout(),
                    user.age()
            ));
        }

        return cards;

    }


}
