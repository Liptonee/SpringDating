package org.petproject.dating_backend.auth;

import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.petproject.dating_backend.common.exception.ConflictException;
import org.petproject.dating_backend.common.exception.ErrorDto;
import org.petproject.dating_backend.common.exception.UnauthorizedException;
import org.petproject.dating_backend.common.security.CustomUserDetails;
import org.petproject.dating_backend.common.security.JwtGenerator;
import org.petproject.dating_backend.common.security.RefreshTokenService;
import org.petproject.dating_backend.user.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Аутентификация", description = "Регистрация, вход, выход, обновление токенов")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtGenerator jwtGenerator;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final RefreshTokenService refreshTokenService;

    private final MeterRegistry registry;

    @Value("${jwt.refresh.cookie.secured}")
    private boolean isSecuredCookie;

    @Operation(
            summary = "Вход в систему",
            description = """
                    Проверяет email и пароль. При успехе возвращает JWT access token в теле
                    и устанавливает raw refresh токен в HttpOnly Cookie "refreshToken".
                    """,
            security = @SecurityRequirement(name = "none")
    )
    @ApiResponse(responseCode = "200", description = "Аутентификация прошла успешно",
            content = @Content(schema = @Schema(implementation = AuthResponseDto.class)))
    @ApiResponse(responseCode = "401", description = "Неверный email или пароль",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        Authentication authentication = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String access = jwtGenerator.generateToken(userDetails.getUser());

        String refresh = refreshTokenService.generateToken();
        refreshTokenService.save(refresh, userDetails.getUser().getEmail());
        ResponseCookie cookie = createRefreshCookie(refresh);

        registry.counter("user.logins.total", "result", "success").increment();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthResponseDto(access));
    }


    @Operation(
            summary = "Регистрация",
            description = """
                    Регистрирует пользователя по email и паролю.
                    Также необходимо имя и пол.
                    """,
            security = @SecurityRequirement(name = "none")
    )
    @ApiResponse(responseCode = "201", description = "Регистрация прошла успешно",
            content = @Content(schema = @Schema(implementation = ProfileUserDto.class)))
    @ApiResponse(responseCode = "409", description = "Email уже используется",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    @PostMapping("/register")
    public ResponseEntity<ProfileUserDto> register(@Valid @RequestBody RegisterRequestDto request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Такой email уже используется", 409);
        }

        UserEntity user = new UserEntity();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setRole(UserRole.USER);
        user.setReadyForDeck(false);
        userRepository.save(user);

        registry.counter("user.registrations.total").increment();
        return ResponseEntity.status(201).body(userMapper.toProfileDto(user));
    }


    @Operation(
            summary = "Обновляет access и refresh токены",
            description = """
                    Принимает из cookie имеющийся refresh токен, удаляет его
                    и возвращает в теле новый access токен и кладёт в cookie новый refresh.
                    """,
            security = @SecurityRequirement(name = "none")
    )
    @ApiResponse(responseCode = "200", description = "Ротация прошла успешно",
            content = @Content(schema = @Schema(implementation = AuthResponseDto.class)))
    @ApiResponse(responseCode = "401", description = "Либо refresh не предоставлен, либо он невалидный (нету в бд/истёк TTL)",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refresh(
            @CookieValue(name = "refreshToken", required = false) String oldRefresh
    ) {
        if (oldRefresh == null) throw new UnauthorizedException("Refresh token не предоставлен", 401);

        String username = refreshTokenService.getUsername(oldRefresh);
        if (username == null) throw new UnauthorizedException("Refresh token не существует", 401);

        UserEntity user = userRepository.findByEmailOrThrow(username);
        String newAccess = jwtGenerator.generateToken(user);
        String newRefresh = refreshTokenService.generateToken();

        refreshTokenService.delete(oldRefresh);
        refreshTokenService.save(newRefresh, user.getEmail());

        ResponseCookie cookie = createRefreshCookie(newRefresh);

        registry.counter("token.refreshes.total").increment();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthResponseDto(newAccess));

    }


    @Operation(summary = "Выход из системы",
            description = """
                    Принимает из cookie refresh токен, удаляет его
                    и возвращает чистые cookie с пустым телом.
                    При этом access токен всё ещё валиден до истечения TTL.
                    """,
            security = @SecurityRequirement(name = "none"))
    @ApiResponse(responseCode = "200", description = "Логаут успешно выполнен")
    @ApiResponse(responseCode = "401", description = "Refresh не предоставлен",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(value = "refreshToken", required = false) String token) {
        if (token == null) throw new UnauthorizedException("Refresh token не предоставлен", 401);

        refreshTokenService.delete(token);

        ResponseCookie clearCookie = createClearCookie();

        registry.counter("user.logouts.total").increment();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .build();
    }

    private ResponseCookie createRefreshCookie(String token) {
        return ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(isSecuredCookie)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(Duration.ofDays(7))
                .build();
    }

    private ResponseCookie createClearCookie() {
        return ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(isSecuredCookie)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(Duration.ZERO)
                .build();

    }

}
