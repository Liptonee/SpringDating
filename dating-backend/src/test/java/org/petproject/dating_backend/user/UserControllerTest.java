package org.petproject.dating_backend.user;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.petproject.dating_backend.common.exception.NotFoundException;
import org.petproject.dating_backend.common.security.JwtAuthenticationFilter;
import org.petproject.dating_backend.common.security.RateLimitFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private RateLimitFilter rateLimitFilter;

    @MockitoBean
    private MeterRegistry meterRegistry;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID, null, List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsProfile_whenGetCurrentUser() {
        ProfileUserDto dto = new ProfileUserDto(
                USER_ID, "Иван", "ivan@example.com", "short", "full",
                (short) 25, "Москва", UserGender.MALE, (short) 20, (short) 30, true
        );
        when(userService.getCurrentUser(USER_ID)).thenReturn(dto);

        assertThat(mvc.get().uri("/api/users/me"))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {
                          "id": 1,
                          "firstName": "Иван",
                          "email": "ivan@example.com",
                          "shortAbout": "short",
                          "fullAbout": "full",
                          "age": 25,
                          "city": "Москва",
                          "gender": "MALE",
                          "preferredAgeMin": 20,
                          "preferredAgeMax": 30,
                          "readyForDeck": true
                        }
                        """);
    }

    @Test
    void returnsNotFound_whenGetCurrentUser() {
        when(userService.getCurrentUser(USER_ID))
                .thenThrow(new NotFoundException("Пользователь не найден", 404));

        assertThat(mvc.get().uri("/api/users/me"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.statusCode").isEqualTo(404);
    }


    @Test
    void getUser_returnsBasicInfo() {
        GetUserDto dto = new GetUserDto(
                2L, "Пётр", "short", "full", (short) 30, "СПб", UserGender.FEMALE
        );
        when(userService.getUser(2L)).thenReturn(dto);

        assertThat(mvc.get().uri("/api/users/2"))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {
                          "id": 2,
                          "firstName": "Пётр",
                          "shortAbout": "short",
                          "fullAbout": "full",
                          "age": 30,
                          "city": "СПб",
                          "gender": "FEMALE"
                        }
                        """);
    }


    @Test
    void patchCurrentUser_updatesProfile() {
        ProfileUserDto response = new ProfileUserDto(
                USER_ID, "Иван", "ivan@example.com", "short", "full",
                (short) 26, "Москва", UserGender.MALE, (short) 20, (short) 30, true
        );
        when(userService.patchCurrentUser(eq(USER_ID), any(ProfileUserDto.class)))
                .thenReturn(response);

        assertThat(mvc.patch().uri("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"firstName":"Иван","age":26}
                        """))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.age").isEqualTo(26);
    }

    @Test
    void patchCurrentUser_invalidAge_returnsBadRequest() {
        assertThat(mvc.patch().uri("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"age":17}
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.title").isEqualTo("Validation failed.");
    }

    @Test
    void patchCurrentUser_idMustBeNull_returnsBadRequest() {
        assertThat(mvc.patch().uri("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"id":5,"firstName":"Иван"}
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.title").isEqualTo("Validation failed.");
    }


    @Test
    void getNotReadyFields_returnsMissingFields() {
        when(userService.getNotReadyFields(USER_ID))
                .thenReturn(Set.of("firstName", "age"));

        assertThat(mvc.get().uri("/api/users/me/completeness"))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        ["firstName","age"]
                        """);
    }
}