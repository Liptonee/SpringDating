package org.petproject.dating_backend.common.security;

import lombok.Getter;
import org.petproject.dating_backend.user.UserEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Getter
public class CustomUserDetails implements UserDetails {
    private final UserEntity user;
    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(UserEntity user, String username, String password, Collection<? extends GrantedAuthority> authorities) {
        this.user = user;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
    }

}
