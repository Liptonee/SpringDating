package org.petproject.dating_backend.user;


import jakarta.persistence.*;
import lombok.Data;
import org.petproject.dating_backend.photo.PhotoEntity;


import java.util.List;


@Entity
@Table(name = "users")
@Data
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 30)
    private String firstName;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, unique = true, length = 50)
    private String email;

    @Column(length = 40)
    private String city;

    @Column(name = "full_about",length = 512)
    private String fullAbout;

    @Column(name = "short_about",length = 127)
    private String shortAbout;

    @Column()
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private UserGender gender;

    @Column(name = "preferred_age_min", nullable = false)
    private Short preferredAgeMin;

    @Column(name = "preferred_age_max", nullable = false)
    private Short preferredAgeMax;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @OneToMany(mappedBy = "user",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<PhotoEntity> photoList;

}
