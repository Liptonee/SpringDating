package org.petproject.dating_backend.match;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;


@Getter
@Setter
@Entity
@Table(name = "matches",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_matches_user_pair",
                columnNames = {"first_user_id", "second_user_id"}
        ),
        indexes = @Index(
                name = "idx_matches_second_user_id",
                columnList = "second_user_id"
        ))
public class MatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_user_id", nullable = false, updatable = false)
    private Long firstUserId;

    @Column(name = "second_user_id", nullable = false, updatable = false)
    private Long secondUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

}
