package org.petproject.dating_backend.common.security;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.petproject.dating_backend.common.exception.NotFoundException;
import org.petproject.dating_backend.user.UserEntity;
import org.petproject.dating_backend.user.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Component
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;


    @Override
    public @NonNull CustomUserDetails loadUserByUsername(@NonNull String username)
            throws NotFoundException {

        UserEntity user = userRepository.findByEmail(username)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден", 404));

        List<GrantedAuthority> authorities = new ArrayList<>(1);

        String role = user.getRole().toString();
        Assert.isTrue(!role.startsWith("ROLE_"),
                () -> role + " cannot start with ROLE_ (it is automatically added)");
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));

        return new CustomUserDetails(
                user,
                user.getEmail(),
                user.getPasswordHash(),
                authorities
        );
    }
}
