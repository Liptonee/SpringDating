package org.petproject.dating_backend.auth;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.petproject.dating_backend.common.exception.ConflictException;
import org.petproject.dating_backend.common.exception.UnauthorizedException;
import org.petproject.dating_backend.common.security.CustomUserDetails;
import org.petproject.dating_backend.common.security.JwtService;
import org.petproject.dating_backend.common.security.RefreshTokenService;
import org.petproject.dating_backend.user.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Long USER_ID = 123L;
    private static final String EMAIL = "ivan@example.com";
    private static final String PASSWORD = "SecurePass123!";

    @Mock
    private AuthenticationManager authManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapper userMapper;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private JwtService jwtService;
    @Mock
    private MeterRegistry registry;
    @Mock
    private Counter counter;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        lenient().when(registry.counter(anyString())).thenReturn(counter);
        lenient().when(registry.counter(anyString(), anyString(), anyString())).thenReturn(counter);
    }


    private static UserEntity user() {
        UserEntity u = new UserEntity();
        u.setId(USER_ID);
        u.setEmail(EMAIL);
        u.setFirstName("Иван");
        u.setPasswordHash("hash");
        u.setRole(UserRole.USER);
        u.setReadyForDeck(false);
        return u;
    }

    private static ProfileUserDto toProfileDto(UserEntity e) {
        return new ProfileUserDto(
                e.getId(), e.getFirstName(), e.getEmail(), e.getShortAbout(),
                e.getFullAbout(), e.getAge(), e.getCity(), e.getGender(),
                e.getPreferredAgeMin(), e.getPreferredAgeMax(), e.getReadyForDeck()
        );
    }

    private void stubAuthentication(UserEntity user) {
        Authentication auth = mock(Authentication.class);
        CustomUserDetails details = mock(CustomUserDetails.class);

        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(auth.getPrincipal()).thenReturn(details);
        when(details.getUser()).thenReturn(user);
    }

    private void stubSuccessfulRegister() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn("hash");
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toProfileDto(any(UserEntity.class)))
                .thenAnswer(inv -> toProfileDto(inv.getArgument(0)));
    }


    @Nested
    class RegisterTest {

        @Test
        void createsUser_savesWithCorrectFields_andIncrementsCounter() {
            stubSuccessfulRegister();

            ProfileUserDto result = authService.register(
                    new RegisterRequestDto(EMAIL, "Иван", PASSWORD));

            assertThat(result.email()).isEqualTo(EMAIL);
            assertThat(result.firstName()).isEqualTo("Иван");
            assertThat(result.readyForDeck()).isFalse();

            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).save(captor.capture());
            UserEntity saved = captor.getValue();
            assertThat(saved.getEmail()).isEqualTo(EMAIL);
            assertThat(saved.getPasswordHash()).isEqualTo("hash");
            assertThat(saved.getFirstName()).isEqualTo("Иван");
            assertThat(saved.getRole()).isEqualTo(UserRole.USER);
            assertThat(saved.getReadyForDeck()).isFalse();

            verify(counter).increment();
        }

        @Test
        void normalizesEmail_toLowerCaseAndTrim() {
            stubSuccessfulRegister();

            authService.register(new RegisterRequestDto(
                    "  Ivan@Example.COM  ", "Иван", PASSWORD));

            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getEmail()).isEqualTo(EMAIL);
            verify(userRepository).existsByEmail(EMAIL);
        }

        @Test
        void throwsConflict_whenEmailAlreadyExists() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> authService.register(
                    new RegisterRequestDto(EMAIL, "Иван", PASSWORD)))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Такой email уже используется");

            verify(userRepository, never()).save(any());
            verify(counter, never()).increment();
        }

        @Test
        void throwsConflict_whenSaveFailsWithDataIntegrityViolation() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(PASSWORD)).thenReturn("hash");
            when(userRepository.save(any(UserEntity.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate"));

            assertThatThrownBy(() -> authService.register(
                    new RegisterRequestDto(EMAIL, "Иван", PASSWORD)))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Такой email уже используется");

            verify(counter, never()).increment();
        }
    }


    @Nested
    class LoginTest {

        @Test
        void returnsTokens_savesRefreshToken_andIncrementsCounter() {
            UserEntity user = user();
            stubAuthentication(user);
            when(jwtService.generateToken(user)).thenReturn("access-token");
            when(refreshTokenService.generateToken()).thenReturn("refresh-token");
            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

            AuthTokens tokens = authService.login(new LoginRequestDto(EMAIL, PASSWORD));

            assertThat(tokens.accessToken()).isEqualTo("access-token");
            assertThat(tokens.refreshToken()).isEqualTo("refresh-token");
            verify(refreshTokenService).save("refresh-token", EMAIL);
            verify(counter).increment();
        }
    }


    @Nested
    class RefreshTest {

        @Test
        void throwsUnauthorized_whenTokenIsNull() {
            assertThatThrownBy(() -> authService.refresh(null))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Refresh token не предоставлен");

            verifyNoInteractions(refreshTokenService, jwtService);
            verify(counter, never()).increment();
        }

        @Test
        void throwsUnauthorized_whenTokenDoesNotExist() {
            when(refreshTokenService.getUsername("old-refresh")).thenReturn(null);

            assertThatThrownBy(() -> authService.refresh("old-refresh"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Refresh token не существует");

            verify(userRepository, never()).findByEmailOrThrow(anyString());
            verify(refreshTokenService, never()).delete(anyString());
            verify(refreshTokenService, never()).save(anyString(), anyString());
            verify(counter, never()).increment();
        }

        @Test
        void rotatesTokens_onSuccess() {
            UserEntity user = user();
            when(refreshTokenService.getUsername("old-refresh")).thenReturn(EMAIL);
            when(userRepository.findByEmailOrThrow(EMAIL)).thenReturn(user);
            when(jwtService.generateToken(user)).thenReturn("new-access");
            when(refreshTokenService.generateToken()).thenReturn("new-refresh");

            AuthTokens tokens = authService.refresh("old-refresh");

            assertThat(tokens.accessToken()).isEqualTo("new-access");
            assertThat(tokens.refreshToken()).isEqualTo("new-refresh");
            verify(refreshTokenService).delete("old-refresh");
            verify(refreshTokenService).save("new-refresh", EMAIL);
            verify(counter).increment();
        }
    }


    @Nested
    class LogoutTest {

        @Test
        void throwsUnauthorized_whenTokenIsNull() {
            assertThatThrownBy(() -> authService.logout(null))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Refresh token не предоставлен");

            verifyNoInteractions(refreshTokenService);
            verify(counter, never()).increment();
        }

        @Test
        void deletesToken_andIncrementsCounter_onSuccess() {
            authService.logout("refresh-token");

            verify(refreshTokenService).delete("refresh-token");
            verify(counter).increment();
        }
    }
}