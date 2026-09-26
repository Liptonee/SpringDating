package org.petproject.dating_backend.common.security;

import lombok.RequiredArgsConstructor;
import org.petproject.dating_backend.common.exception.InternalServerException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    @Value("${jwt.expiration.refresh}")
    private long REFRESH_TTL;

    public void save(String token, String username) {
        String hash = sha256(token);
        redisTemplate.opsForValue().set(
                "refresh:" + hash,
                username,
                Duration.ofSeconds(REFRESH_TTL)
        );
    }

    public void delete(String token) {
        String hash = sha256(token);
        redisTemplate.delete("refresh:" + hash);
    }


    public String getUsername(String token) {
        String hash = sha256(token);
        return redisTemplate.opsForValue().get("refresh:" + hash);
    }

    public String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return encoder.encodeToString(bytes);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encoded);
        } catch (NoSuchAlgorithmException e) {
            throw new InternalServerException("Ошибка при хэшировании refresh token", e);
        }
    }

}
