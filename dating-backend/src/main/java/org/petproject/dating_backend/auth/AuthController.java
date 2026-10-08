package org.petproject.dating_backend.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.petproject.dating_backend.common.exception.ErrorDto;
import org.petproject.dating_backend.user.ProfileUserDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Аутентификация", description = "Регистрация, вход, выход, обновление токенов")
public class AuthController {

    private final AuthService authService;

    @Value("${jwt.refresh.cookie.secured}")
    private boolean isSecuredCookie;


    @Operation(
            summary = "Регистрация",
            description = """
                    Регистрирует пользователя по email и паролю.
                    Также необходимо имя и пол.
                    """
    )
    @ApiResponse(responseCode = "201", description = "Регистрация прошла успешно",
            content = @Content(schema = @Schema(implementation = ProfileUserDto.class)))
    @ApiResponse(responseCode = "409", description = "Email уже используется",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    @PostMapping("/register")
    public ResponseEntity<ProfileUserDto> register(@Valid @RequestBody RegisterRequestDto request) {
        ProfileUserDto profile = authService.register(request);
        return ResponseEntity.status(201).body(profile);
    }


    @Operation(
            summary = "Вход в систему",
            description = """
                    Проверяет email и пароль. При успехе возвращает JWT access token в теле
                    и устанавливает raw refresh токен в HttpOnly Cookie "refreshToken".
                    """
    )
    @ApiResponse(responseCode = "200", description = "Аутентификация прошла успешно",
            content = @Content(schema = @Schema(implementation = AuthResponseDto.class)))
    @ApiResponse(responseCode = "401", description = "Неверный email или пароль",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        AuthTokens tokens = authService.login(request);

        ResponseCookie cookie = createRefreshCookie(tokens.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthResponseDto(tokens.accessToken()));
    }


    @Operation(
            summary = "Обновляет access и refresh токены",
            description = """
                    Принимает из cookie имеющийся refresh токен, удаляет его
                    и возвращает в теле новый access токен и кладёт в cookie новый refresh.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Ротация прошла успешно",
            content = @Content(schema = @Schema(implementation = AuthResponseDto.class)))
    @ApiResponse(responseCode = "401", description = "Либо refresh не предоставлен, либо он невалидный (нету в бд/истёк TTL)",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    @PostMapping("/refresh")
    //Недоработка - при повторном использовании уже удалённого refresh-токена нужно инвалидировать все сессии пользователя.
    public ResponseEntity<AuthResponseDto> refresh(
            @CookieValue(name = "refreshToken", required = false) String oldRefresh
    ) {
        AuthTokens tokens = authService.refresh(oldRefresh);

        ResponseCookie cookie = createRefreshCookie(tokens.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthResponseDto(tokens.accessToken()));
    }

    @Operation(summary = "Выход из системы",
            description = """
                    Принимает из cookie refresh токен, удаляет его
                    и возвращает чистые cookie с пустым телом.
                    При этом access токен всё ещё валиден до истечения TTL.
                    """)
    @ApiResponse(responseCode = "200", description = "Логаут успешно выполнен")
    @ApiResponse(responseCode = "401", description = "Refresh не предоставлен",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = "refreshToken", required = false) String token
    ) {
        authService.logout(token);

        ResponseCookie clearCookie = createClearCookie();

        return ResponseEntity.status(204)
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .build();
    }

    private ResponseCookie createRefreshCookie(String token) {
        return ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(isSecuredCookie)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(Duration.ofDays(7))
                .build();
    }

    private ResponseCookie createClearCookie() {
        return ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(isSecuredCookie)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(Duration.ZERO)
                .build();
    }
}