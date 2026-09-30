package org.petproject.dating_backend.user;


import jakarta.persistence.LockModeType;
import org.petproject.dating_backend.common.exception.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

   default UserEntity findByIdOrThrow(Long id) throws NotFoundException{
      return findById(id).orElseThrow(
              () -> new NotFoundException("Пользователь не найден", 404)
      );
   }

   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("select u from UserEntity u where u.id = :id")
   Optional<UserEntity> findByIdForUpdate(@Param("id") Long id);

}
