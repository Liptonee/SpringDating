package org.petproject.dating_backend.photo;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.petproject.dating_backend.user.UserEntity;

import java.time.Instant;

@Entity
@Table(name = "photos")
@Getter
@Setter
public class PhotoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "object_name", nullable = false)
    private String objectName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size", nullable = false)
    private Long size;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt = Instant.now();

}
