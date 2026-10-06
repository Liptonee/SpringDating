package org.petproject.dating_backend.user;


import jakarta.persistence.LockModeType;
import org.petproject.dating_backend.common.exception.NotFoundException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    default UserEntity findByEmailOrThrow(String email) throws NotFoundException {
        return findByEmail(email).orElseThrow(
                () -> new NotFoundException("Пользователь не найден", 404)
        );
    }

    default UserEntity findByIdOrElseThrow(Long id) throws NotFoundException {
        return findById(id).orElseThrow(
                () -> new NotFoundException("Пользователь не найден", 404)
        );
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u where u.id = :id")
    Optional<UserEntity> findByIdForUpdate(@Param("id") Long id);

    @Query("""
                select u from UserEntity u
                where u.id <> :curUserId
                  and u.age between :ageMin and :ageMax
                  and u.gender = :gender
                  and u.city = :city
                  and u.id not in (
                      select s.toId from SwipeEntity s where s.fromId = :curUserId
                  )
            """)
    List<UserEntity> findByPreferences(
            Long curUserId,
            UserGender gender,
            Short ageMin,
            Short ageMax,
            String city,
            Pageable pageable
    );

    @Query("""
                select u.firstName from UserEntity u
                where u.id = :userId
            """)
    Optional<String> getFirstNameById(@Param("userId") Long userId);

}
