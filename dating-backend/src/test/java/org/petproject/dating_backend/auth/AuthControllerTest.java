package org.petproject.dating_backend.auth;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.petproject.dating_backend.common.exception.ConflictException;
import org.petproject.dating_backend.common.exception.UnauthorizedException;
import org.petproject.dating_backend.common.security.JwtService;
import org.petproject.dating_backend.user.ProfileUserDto;
import org.petproject.dating_backend.user.UserGender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "jwt.refresh.cookie.secured=false")
class AuthControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private MeterRegistry meterRegistry;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private ProxyManager<String> proxyManager;

    @BeforeEach
    void stubMeterRegistry() {
        when(meterRegistry.counter(anyString(), any(String[].class)))
                .thenReturn(mock(Counter.class));
    }


    @Test
    void register_shouldReturn201AndProfile() {
        ProfileUserDto profileUserDto = new ProfileUserDto(
                1L, "John", "test@example.com", "short", "full",
                (short) 25, "Москва", UserGender.MALE, (short) 20, (short) 30, true
        );
        when(authService.register(any(RegisterRequestDto.class)))
                .thenReturn(profileUserDto);

        MvcTestResult result = mvc.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "test@example.com",
                          "firstName": "John",
                          "password": "password123"
                        }
                        """)
                .exchange();

        assertThat(result).hasStatus(201);
        assertThat(result).bodyJson()
                .extractingPath("$.email").isEqualTo("test@example.com");
    }

    @Test
    void register_shouldReturn409WhenEmailExists() {
        when(authService.register(any(RegisterRequestDto.class)))
                .thenThrow(new ConflictException("Такой email уже используется", 409));

        assertThat(mvc.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "test@example.com",
                          "firstName": "John",
                          "password": "password123"
                        }
                        """)
                .exchange())
                .hasStatus(409)
                .bodyJson()
                .extractingPath("$.title").isEqualTo("Conflict data");
    }


    @Test
    void login_shouldReturn200WithAccessTokenAndRefreshCookie() {
        when(authService.login(any(LoginRequestDto.class)))
                .thenReturn(new AuthTokens("access-token-123", "refresh-token-456"));

        MvcTestResult result = mvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "test@example.com",
                          "password": "password123"
                        }
                        """)
                .exchange();

        assertThat(result)
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.token").isEqualTo("access-token-123");

        Cookie refreshCookie = result.getResponse().getCookie("refreshToken");
        assertThat(refreshCookie).isNotNull();
        assertThat(refreshCookie.getValue()).isEqualTo("refresh-token-456");
        assertThat(refreshCookie.isHttpOnly()).isTrue();
        assertThat(refreshCookie.getSecure()).isFalse();
        assertThat(refreshCookie.getPath()).isEqualTo("/api/auth");
        assertThat(refreshCookie.getMaxAge()).isEqualTo(7 * 24 * 60 * 60);
    }

    @Test
    void login_shouldReturn401WhenBadCredentials() {
        when(authService.login(any(LoginRequestDto.class)))
                .thenThrow(new BadCredentialsException("Неверный email или пароль"));

        assertThat(mvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "test@example.com",
                          "password": "wrong-password"
                        }
                        """)
                .exchange())
                .hasStatus(401)
                .bodyJson()
                .extractingPath("$.title").isEqualTo("Invalid login or password");
    }


    @Test
    void refresh_shouldReturn200WithNewTokensAndNewCookie() {
        when(authService.refresh("old-refresh-token"))
                .thenReturn(new AuthTokens("new-access-token", "new-refresh-token"));

        MvcTestResult result = mvc.post().uri("/api/auth/refresh")
                .cookie(new Cookie("refreshToken", "old-refresh-token"))
                .exchange();

        assertThat(result)
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.token").isEqualTo("new-access-token");

        Cookie newCookie = result.getResponse().getCookie("refreshToken");
        assertThat(newCookie).isNotNull();
        assertThat(newCookie.getValue()).isEqualTo("new-refresh-token");
        assertThat(newCookie.isHttpOnly()).isTrue();
        assertThat(newCookie.getPath()).isEqualTo("/api/auth");
        assertThat(newCookie.getMaxAge()).isEqualTo(7 * 24 * 60 * 60);
    }

    @Test
    void refresh_shouldReturn401WhenCookieMissing() {
        when(authService.refresh(null))
                .thenThrow(new UnauthorizedException("Refresh token не предоставлен", 401));

        assertThat(mvc.post().uri("/api/auth/refresh")
                .exchange())
                .hasStatus(401)
                .bodyJson()
                .extractingPath("$.title").isEqualTo("Unauthorized");
    }


    @Test
    void logout_shouldReturn204AndClearCookie() {
        MvcTestResult result = mvc.post().uri("/api/auth/logout")
                .cookie(new Cookie("refreshToken", "some-refresh-token"))
                .exchange();

        assertThat(result).hasStatus(204);

        Cookie clearedCookie = result.getResponse().getCookie("refreshToken");
        assertThat(clearedCookie).isNotNull();
        assertThat(clearedCookie.getValue()).isEmpty();
        assertThat(clearedCookie.getMaxAge()).isZero();
        assertThat(clearedCookie.getPath()).isEqualTo("/api/auth");
        assertThat(clearedCookie.isHttpOnly()).isTrue();
    }

    @Test
    void logout_shouldReturn401WhenCookieMissing() {
        doThrow(new UnauthorizedException("Refresh token не предоставлен", 401))
                .when(authService).logout(null);

        assertThat(mvc.post().uri("/api/auth/logout")
                .exchange())
                .hasStatus(401)
                .bodyJson()
                .extractingPath("$.title").isEqualTo("Unauthorized");
    }
}