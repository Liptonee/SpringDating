package org.petproject.dating_backend.common.security;

import io.jsonwebtoken.Jwts;
import org.petproject.dating_backend.user.UserEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
public class JwtGenerator extends JwtService {

    @Value("${jwt.expiration.access}")
    private Long expiration;

    public String generateToken(UserEntity user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim(JwtClaims.USER_ID, user.getId())
                .claim(JwtClaims.ROLES, List.of(user.getRole().name()))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignKey())
                .compact();
    }


}
