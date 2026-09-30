package org.petproject.dating_backend.deck;

public record CardDto(
        Long userId,
        String mainPhotoUrl,
        String firstName,
        String shortAbout,
        Short age
) {
}
