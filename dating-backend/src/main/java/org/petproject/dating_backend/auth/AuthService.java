package org.petproject.dating_backend.auth;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final MeterRegistry registry;

    @Transactional
    public ProfileUserDto register(RegisterRequestDto request) {
        String normalizedEmail = request.email().toLowerCase().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Такой email уже используется", 409);
        }

        UserEntity user = new UserEntity();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setRole(UserRole.USER);
        user.setReadyForDeck(false);

        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Такой email уже используется", 409);
        }

        registry.counter("user.registrations.total").increment();

        return userMapper.toProfileDto(user);
    }

    @Transactional(readOnly = true)
    public AuthTokens login(LoginRequestDto request) {
        Authentication authentication = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String access = jwtService.generateToken(userDetails.getUser());

        String refresh = refreshTokenService.generateToken();
        refreshTokenService.save(refresh, userDetails.getUser().getEmail());

        registry.counter("user.logins.total", "result", "success").increment();

        return new AuthTokens(access, refresh);
    }


    @Transactional(readOnly = true)
    public AuthTokens refresh(String oldRefresh) {
        if (oldRefresh == null) {
            throw new UnauthorizedException("Refresh token не предоставлен", 401);
        }

        String username = refreshTokenService.getUsername(oldRefresh);
        if (username == null) {
            throw new UnauthorizedException("Refresh token не существует", 401);
        }

        UserEntity user = userRepository.findByEmailOrThrow(username);

        String newAccess = jwtService.generateToken(user);
        String newRefresh = refreshTokenService.generateToken();

        refreshTokenService.delete(oldRefresh);
        refreshTokenService.save(newRefresh, user.getEmail());

        registry.counter("token.refreshes.total").increment();

        return new AuthTokens(newAccess, newRefresh);
    }

    public void logout(String token) {
        if (token == null) {
            throw new UnauthorizedException("Refresh token не предоставлен", 401);
        }

        refreshTokenService.delete(token);
        registry.counter("user.logouts.total").increment();
    }
}