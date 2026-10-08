package org.petproject.dating_backend.auth;

public record AuthTokens(
        String accessToken,
        String refreshToken
) {
}