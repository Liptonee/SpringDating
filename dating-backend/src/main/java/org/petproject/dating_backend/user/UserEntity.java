package org.petproject.dating_backend.user;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "users")
@Setter
@Getter
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 30)
    private String firstName;

    @Column(nullable = false, unique = true, length = 50)
    private String email;

    @Column(name = "full_about", length = 512)
    private String fullAbout;

    @Column(name = "short_about", length = 127)
    private String shortAbout;

    @Column()
    private Short age;

    @Column(length = 40)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private UserGender gender;

    @Column(name = "preferred_age_min")
    private Short preferredAgeMin;

    @Column(name = "preferred_age_max")
    private Short preferredAgeMax;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "ready_for_deck", nullable = false)
    private Boolean readyForDeck;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

}
