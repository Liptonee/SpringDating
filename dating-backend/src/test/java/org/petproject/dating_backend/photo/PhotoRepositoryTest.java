package org.petproject.dating_backend.photo;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.petproject.dating_backend.common.exception.NotFoundException;
import org.petproject.dating_backend.user.UserEntity;
import org.petproject.dating_backend.user.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class PhotoRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    private static final AtomicLong COUNTER = new AtomicLong();

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private TestEntityManager testEntityManager;


    private UserEntity persistUser() {
        return testEntityManager.persistAndFlush(user());
    }

    private PhotoEntity persist(PhotoEntity photo) {
        return testEntityManager.persistAndFlush(photo);
    }

    private void flushAndClear() {
        testEntityManager.flush();
        testEntityManager.getEntityManager().clear();
    }

    private PhotoEntity reload(Long id) {
        return testEntityManager.getEntityManager().find(PhotoEntity.class, id);
    }

    private static UserEntity user() {
        UserEntity u = new UserEntity();
        u.setFirstName("User");
        u.setEmail("user-" + COUNTER.incrementAndGet() + "@example.com");
        u.setPasswordHash("hash");
        u.setReadyForDeck(true);
        u.setRole(UserRole.USER);
        return u;
    }

    private static PhotoEntity photo(Long userId, boolean isMain) {
        return photo(userId, isMain, Instant.now());
    }

    private static PhotoEntity photo(Long userId, boolean isMain, Instant uploadedAt) {
        PhotoEntity p = new PhotoEntity();
        p.setUserId(userId);
        p.setObjectName("obj-" + COUNTER.incrementAndGet());
        p.setOriginalName("photo.jpg");
        p.setContentType("image/jpeg");
        p.setSize(1024L);
        p.setMain(isMain);
        p.setUploadedAt(uploadedAt);
        return p;
    }


    @Nested
    class CountByUserIdTest {

        @Test
        void returnsNumberOfPhotosForUser() {
            Long userId = persistUser().getId();
            Long otherUserId = persistUser().getId();

            persist(photo(userId, true));
            persist(photo(userId, false));
            persist(photo(otherUserId, true));
            flushAndClear();

            int result = photoRepository.countByUserId(userId);

            assertThat(result).isEqualTo(2);
        }

        @Test
        void returnsZero_whenUserHasNoPhotos() {
            Long userId = persistUser().getId();
            flushAndClear();

            int result = photoRepository.countByUserId(userId);

            assertThat(result).isZero();
        }
    }


    @Nested
    class FindByUserIdOrderByUploadedAtDescTest {

        @Test
        void returnsPhotosOfUserOrderedByUploadedAtDesc() {
            Long userId = persistUser().getId();
            Long otherUserId = persistUser().getId();
            Instant now = Instant.now();

            PhotoEntity oldest = persist(photo(userId, false, now.minusSeconds(30)));
            PhotoEntity middle = persist(photo(userId, false, now.minusSeconds(20)));
            PhotoEntity newest = persist(photo(userId, true, now.minusSeconds(10)));
            persist(photo(otherUserId, true, now));
            flushAndClear();

            List<PhotoEntity> result = photoRepository.findByUserIdOrderByUploadedAtDesc(userId);

            assertThat(result)
                    .extracting(PhotoEntity::getId)
                    .containsExactly(newest.getId(), middle.getId(), oldest.getId());
        }

        @Test
        void returnsEmpty_whenUserHasNoPhotos() {
            Long userId = persistUser().getId();
            flushAndClear();

            List<PhotoEntity> result = photoRepository.findByUserIdOrderByUploadedAtDesc(userId);

            assertThat(result).isEmpty();
        }
    }


    @Nested
    class FindByIdOrThrowTest {

        @Test
        void returnsPhoto_whenExists() {
            Long userId = persistUser().getId();
            PhotoEntity saved = persist(photo(userId, true));
            flushAndClear();

            PhotoEntity result = photoRepository.findByIdOrThrow(saved.getId());

            assertThat(result.getId()).isEqualTo(saved.getId());
        }

        @Test
        void throwsNotFound_whenPhotoDoesNotExist() {
            assertThatThrownBy(() -> photoRepository.findByIdOrThrow(999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Фото не найдено");
        }
    }


    @Nested
    class ExistsByUserIdAndIsMainTrueTest {

        @Test
        void returnsTrue_whenUserHasMainPhoto() {
            Long userId = persistUser().getId();
            persist(photo(userId, true));
            flushAndClear();

            boolean result = photoRepository.existsByUserIdAndIsMainTrue(userId);

            assertThat(result).isTrue();
        }

        @Test
        void returnsFalse_whenUserHasNoMainPhoto() {
            Long userId = persistUser().getId();
            persist(photo(userId, false));
            flushAndClear();

            boolean result = photoRepository.existsByUserIdAndIsMainTrue(userId);

            assertThat(result).isFalse();
        }
    }


    @Nested
    class SetNewMainPhotoTest {

        @Test
        void setsNewMainAndUnsetsPrevious() {
            Long userId = persistUser().getId();
            PhotoEntity oldMain = persist(photo(userId, true));
            PhotoEntity newMain = persist(photo(userId, false));
            flushAndClear();

            photoRepository.setNewMainPhoto(userId, newMain.getId());
            flushAndClear();

            assertThat(reload(oldMain.getId()).isMain()).isFalse();
            assertThat(reload(newMain.getId()).isMain()).isTrue();
        }

        @Test
        void setsMain_whenUserHasNoCurrentMainPhoto() {
            Long userId = persistUser().getId();
            PhotoEntity target = persist(photo(userId, false));
            flushAndClear();

            photoRepository.setNewMainPhoto(userId, target.getId());
            flushAndClear();

            assertThat(reload(target.getId()).isMain()).isTrue();
        }

        @Test
        void doesNotAffectOtherUsersMainPhoto() {
            Long user1 = persistUser().getId();
            Long user2 = persistUser().getId();

            PhotoEntity user1Old = persist(photo(user1, true));
            PhotoEntity user1New = persist(photo(user1, false));
            PhotoEntity user2Main = persist(photo(user2, true));
            flushAndClear();

            photoRepository.setNewMainPhoto(user1, user1New.getId());
            flushAndClear();

            assertThat(reload(user1Old.getId()).isMain()).isFalse();
            assertThat(reload(user1New.getId()).isMain()).isTrue();
            assertThat(reload(user2Main.getId()).isMain()).isTrue();
        }
    }


    @Nested
    class FindMainPhotosByUserIdsTest {

        @Test
        void returnsMainPhotoForEachUser() {
            Long user1 = persistUser().getId();
            Long user2 = persistUser().getId();
            Long user3 = persistUser().getId();

            PhotoEntity main1 = persist(photo(user1, true));
            persist(photo(user1, false));
            PhotoEntity main2 = persist(photo(user2, true));
            persist(photo(user3, false)); // у 3-го нет главного
            flushAndClear();

            List<PhotoEntity> result = photoRepository.findMainPhotosByUserIds(
                    List.of(user1, user2, user3)
            );

            assertThat(result)
                    .extracting(PhotoEntity::getId)
                    .containsExactlyInAnyOrder(main1.getId(), main2.getId());
        }

        @Test
        void returnsEmpty_whenNoUsersHaveMainPhotos() {
            Long user1 = persistUser().getId();
            Long user2 = persistUser().getId();
            persist(photo(user1, false));
            persist(photo(user2, false));
            flushAndClear();

            List<PhotoEntity> result = photoRepository.findMainPhotosByUserIds(
                    List.of(user1, user2)
            );

            assertThat(result).isEmpty();
        }
    }


    @Nested
    class FindMainTest {

        @Test
        void returnsMainPhoto_whenExists() {
            Long userId = persistUser().getId();
            PhotoEntity main = persist(photo(userId, true));
            persist(photo(userId, false));
            flushAndClear();

            Optional<PhotoEntity> result = photoRepository.findMain(userId);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(main.getId());
        }

        @Test
        void returnsEmpty_whenUserHasNoMainPhoto() {
            Long userId = persistUser().getId();
            persist(photo(userId, false));
            flushAndClear();

            Optional<PhotoEntity> result = photoRepository.findMain(userId);

            assertThat(result).isEmpty();
        }
    }
}