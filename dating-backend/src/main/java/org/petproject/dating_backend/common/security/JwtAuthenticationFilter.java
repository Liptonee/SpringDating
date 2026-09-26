package org.petproject.dating_backend.common.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(BEARER_PREFIX.length());

        try {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                authenticateFromToken(jwt, request);
            }
            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            log.warn("JWT expired for request {}: {}", request.getRequestURI(), e.getMessage());
            sendError(response, 401, "JWT Token is invalid or expired", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT malformed for request {}: {}", request.getRequestURI(), e.getMessage());
            sendError(response, 401, "JWT Token is invalid or expired", e.getMessage());
        } catch (JwtException e) {
            log.warn("JWT rejected for request {}: {}", request.getRequestURI(), e.getMessage());
            sendError(response, 401, "JWT Token is invalid or expired", e.getMessage());
        }
    }

    private void authenticateFromToken(String jwt, HttpServletRequest request) {
        Long userId = jwtService.extractUserId(jwt);
        if (userId == null || jwtService.isExpired(jwt)) {
            return;
        }

        List<String> roles = jwtService.extractRoles(jwt);
        var authorities = (roles == null ? List.<String>of() : roles).stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList();

        var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void sendError(HttpServletResponse response, int status, String title, String details) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format(
                "{\"statusCode\":%d,\"title\":\"%s\",\"details\":\"%s\"}",
                status, title, details
        ));
    }
}