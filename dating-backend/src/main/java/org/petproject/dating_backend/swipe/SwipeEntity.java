package org.petproject.dating_backend.swipe;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "swipes")
@Getter
@Setter
public class SwipeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_id", nullable = false)
    private Long fromId;

    @Column(name = "to_id", nullable = false)
    private Long toId;

    @Enumerated(EnumType.STRING)
    @Column(name  = "action", nullable = false, length = 20)
    private SwipeAction action;

}
