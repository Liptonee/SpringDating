package org.petproject.dating_backend.common.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {


    private static final Long USER_ID = 123L;
    private static final String TOKEN = "header.payload.signature";


    @Mock
    private JwtService jwtService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private StringWriter responseBody;

    @BeforeEach
    void setUp() throws IOException {
        SecurityContextHolder.clearContext();
        responseBody = new StringWriter();
        lenient().when(response.getWriter()).thenReturn(new PrintWriter(responseBody, true));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void stubBearerHeader() {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + TOKEN);
    }

    private void stubValidToken() {
        when(jwtService.extractUserId(TOKEN)).thenReturn(USER_ID);
        when(jwtService.isExpired(TOKEN)).thenReturn(false);
        when(jwtService.extractRoles(TOKEN)).thenReturn(List.of("USER"));
    }


    @Nested
    class PathThroughTest {

        @Test
        void passesThrough_whenAuthorizationHeaderMissing() throws ServletException, IOException {
            when(request.getHeader("Authorization")).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtService);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        void passesThrough_whenHeaderDoesNotStartWithBearer() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Something dXNlcjpwYXNz");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtService);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }


    }

    @Nested
    class AuthenticateSuccessTest {

        @Test
        void setsAuthentication_whenTokenIsValid() throws ServletException, IOException {
            stubBearerHeader();
            stubValidToken();

            filter.doFilterInternal(request, response, filterChain);

            var authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNotNull();
            assertThat(authentication.getPrincipal()).isEqualTo(USER_ID);
            assertThat(authentication.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_USER");
            assertThat(authentication.getDetails()).isNotNull();

            verify(filterChain).doFilter(request, response);

        }

        @Test
        void setsEmptyAuthorities_whenRolesAreNull() throws Exception {
            stubBearerHeader();
            when(jwtService.extractUserId(TOKEN)).thenReturn(USER_ID);
            when(jwtService.isExpired(TOKEN)).thenReturn(false);
            when(jwtService.extractRoles(TOKEN)).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            var authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNotNull();
            assertThat(authentication.getAuthorities()).isEmpty();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        void skipsAuthentication_whenAlreadyAuthenticated() throws Exception {
            stubBearerHeader();
            var existing = new UsernamePasswordAuthenticationToken(999L, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(existing);

            filter.doFilterInternal(request, response, filterChain);

            verify(jwtService, never()).extractUserId(anyString());
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
        }

    }

    @Nested
    class AuthenticateFailTest {

        @Test
        void doesNotSetAuthentication_whenUserIdIsNull() throws Exception {
            stubBearerHeader();
            when(jwtService.extractUserId(TOKEN)).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            verify(jwtService, never()).isExpired(anyString());
            verify(jwtService, never()).extractRoles(anyString());
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        void doesNotSetAuthentication_whenTokenIsExpiredByServiceCheck() throws Exception {
            stubBearerHeader();
            when(jwtService.extractUserId(TOKEN)).thenReturn(USER_ID);
            when(jwtService.isExpired(TOKEN)).thenReturn(true);

            filter.doFilterInternal(request, response, filterChain);

            verify(jwtService, never()).extractRoles(anyString());
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        void returns401_whenTokenExpired() throws Exception {
            stubBearerHeader();
            when(jwtService.extractUserId(TOKEN))
                    .thenThrow(new ExpiredJwtException(null, null, "expired"));

            filter.doFilterInternal(request, response, filterChain);

            verify(response).setStatus(401);
            verify(response).setContentType("application/json;charset=UTF-8");
            assertThat(responseBody.toString())
                    .contains("\"statusCode\":401")
                    .contains("JWT Token is invalid or expired")
                    .contains("expired");
            verify(filterChain, never()).doFilter(any(), any());
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        void returns401_whenTokenMalformed() throws Exception {
            stubBearerHeader();
            when(jwtService.extractUserId(TOKEN))
                    .thenThrow(new MalformedJwtException("bad format"));

            filter.doFilterInternal(request, response, filterChain);

            verify(response).setStatus(401);
            assertThat(responseBody.toString())
                    .contains("\"statusCode\":401")
                    .contains("bad format");
            verify(filterChain, never()).doFilter(any(), any());
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        void returns401_onGenericJwtException() throws Exception {
            stubBearerHeader();
            when(jwtService.extractUserId(TOKEN))
                    .thenThrow(new JwtException("generic"));

            filter.doFilterInternal(request, response, filterChain);

            verify(response).setStatus(401);
            assertThat(responseBody.toString())
                    .contains("\"statusCode\":401")
                    .contains("generic");
            verify(filterChain, never()).doFilter(any(), any());
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

    }

}