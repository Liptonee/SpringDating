package org.petproject.dating_backend.common.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.petproject.dating_backend.user.UserEntity;
import org.petproject.dating_backend.user.UserGender;
import org.petproject.dating_backend.user.UserRepository;
import org.petproject.dating_backend.user.UserRole;
import org.springframework.security.core.GrantedAuthority;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    CustomUserDetailsService service;

    private static UserEntity readyUser() {
        UserEntity u = new UserEntity();
        u.setId(123L);
        u.setFirstName("Иван");
        u.setEmail("ivan@example.com");
        u.setPasswordHash("hash");
        u.setReadyForDeck(true);
        u.setGender(UserGender.MALE);
        u.setAge((short) 25);
        u.setCity("Москва");
        u.setShortAbout("Кратко");
        u.setPreferredAgeMin((short) 20);
        u.setPreferredAgeMax((short) 30);
        u.setFullAbout("Полно");
        u.setRole(UserRole.USER);
        return u;
    }


    @Test
    void returnCorrectUser_whenCorrectInput() {
        UserEntity user = readyUser();
        when(userRepository.findByEmail("ivan@example.com")).thenReturn(Optional.of(user));

        CustomUserDetails userDetails = service.loadUserByUsername("ivan@example.com");

        assertThat(userDetails).extracting(CustomUserDetails::getUser,
                CustomUserDetails::getPassword,
                CustomUserDetails::getUsername)
                .containsExactly(user, "hash", "ivan@example.com");
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }
}