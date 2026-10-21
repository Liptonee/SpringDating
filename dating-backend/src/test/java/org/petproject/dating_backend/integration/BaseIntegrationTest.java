package org.petproject.dating_backend.integration;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.AfterEach;
import org.petproject.dating_backend.auth.AuthResponseDto;
import org.petproject.dating_backend.auth.LoginRequestDto;
import org.petproject.dating_backend.auth.RegisterRequestDto;
import org.petproject.dating_backend.photo.PhotoEntity;
import org.petproject.dating_backend.photo.PhotoRepository;
import org.petproject.dating_backend.user.ProfileUserDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureRestTestClient
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    private PhotoRepository photoRepository;
    @Autowired
    private CacheManager cacheManager;

    protected static final String EMAIL = "TeStEMAIl@example.com";
    protected static final String FIRST_NAME = "Ivan";
    protected static final String PASSWORD = "SecurePass123!";

    @Container
    @ServiceConnection
    protected static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    protected static RedisContainer redis =
            new RedisContainer("redis:7-alpine");

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected StringRedisTemplate redisTemplate;

    @Autowired
    protected RestTestClient restTestClient;

    protected Long registerAndGetId(String email, String firstName, String rawPassword) {
        RegisterRequestDto request = new RegisterRequestDto(email, firstName, rawPassword);

        return restTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProfileUserDto.class)
                .returnResult()
                .getResponseBody().id();
    }

    protected void register(String email, String firstName, String rawPassword) {
        RegisterRequestDto request = new RegisterRequestDto(email, firstName, rawPassword);

        restTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProfileUserDto.class);
    }

    protected String loginAndGetAccessToken(String email, String password) {
        LoginRequestDto request = new LoginRequestDto(email, password);

        var result = restTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponseDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(result.token()).isNotBlank();

        return result.token();
    }

    protected String loginAndGetRefreshToken(String email, String password) {
        LoginRequestDto request = new LoginRequestDto(email, password);

        var result = restTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponseDto.class)
                .returnResult();

        var cookie = result.getResponseCookies().getFirst("refreshToken");
        assertThat(cookie).isNotNull();

        String refreshToken = cookie.getValue();
        assertThat(refreshToken).isNotBlank();

        return refreshToken;
    }

    protected void saveMockPhoto(Long userId) {
        PhotoEntity photo = new PhotoEntity();
        photo.setMain(true);
        photo.setContentType(userId + "mock");
        photo.setObjectName(userId + "mock");
        photo.setOriginalName(userId + "mock");
        photo.setUserId(userId);
        photo.setSize(userId + 10L);
        photoRepository.save(photo);
        Optional.ofNullable(cacheManager.getCache("photos")).ifPresent(Cache::clear);
    }


    @AfterEach
    protected void cleanUp() {
        jdbc.execute("""
                TRUNCATE TABLE users, photos, matches, swipes, notifications, chat_rooms, chat_messages
                RESTART IDENTITY CASCADE
                """);
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
    }
}