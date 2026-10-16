package org.petproject.dating_backend.photo;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.MinioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.petproject.dating_backend.common.exception.BadRequestException;
import org.petproject.dating_backend.common.exception.ForbiddenException;
import org.petproject.dating_backend.common.exception.NotFoundException;
import org.petproject.dating_backend.common.exception.PhotoException;
import org.petproject.dating_backend.user.UserEntity;
import org.petproject.dating_backend.user.UserGender;
import org.petproject.dating_backend.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.petproject.dating_backend.photo.PhotoService.MAX_PHOTOS;
import static org.petproject.dating_backend.photo.PhotoService.MAX_SIZE_BYTES;

@ExtendWith(MockitoExtension.class)
class PhotoServiceTest {

    @Mock
    private PhotoRepository photoRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MinioClient minio;
    @Mock
    private PhotoMapper photoMapper;
    @Mock
    private MeterRegistry registry;
    @Mock
    private Counter counter;
    @Captor
    private ArgumentCaptor<PhotoEntity> photoEntityCaptor;

    PhotoService photoService;

    private static final String BUCKET_NAME = "test-bucket";
    private static final Long USER_ID = 123L;
    private static final MockMultipartFile FILE = new MockMultipartFile(
            "file",
            "avatar.jpg",
            "image/jpeg",
            "mock-data".getBytes());

    @BeforeEach
    void setUp() {
        photoService = new PhotoService(
                BUCKET_NAME,
                photoRepository,
                userRepository,
                minio,
                photoMapper,
                registry
        );
        lenient().when(registry.counter(any())).thenReturn(counter);
    }

    private static UserEntity readyUser() {
        UserEntity u = new UserEntity();
        u.setId(USER_ID);
        u.setFirstName("Иван");
        u.setEmail("ivan@example.com");
        u.setPasswordHash("hash");
        u.setReadyForDeck(true);
        u.setGender(UserGender.MALE);
        u.setAge((short) 25);
        u.setCity("Москва");
        u.setShortAbout("Кратко");
        u.setPreferredAgeMin((short) 20);
        u.setPreferredAgeMax((short) 30);
        u.setFullAbout("Полно");
        return u;
    }


    private static PhotoEntity photoEntity(Long id, Long userId, String objectName, boolean isMain) {
        PhotoEntity p = new PhotoEntity();
        p.setId(id);
        p.setUserId(userId);
        p.setObjectName(objectName);
        p.setOriginalName("photo.jpg");
        p.setContentType("image/jpeg");
        p.setSize(1024L);
        p.setMain(isMain);
        return p;
    }


    @Nested
    class UploadPhotoTest {

        private void stubUpload() {
            when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(readyUser()));
            when(photoRepository.countByUserId(USER_ID)).thenReturn(0);
            lenient().when(photoRepository.existsByUserIdAndIsMainTrue(USER_ID)).thenReturn(false);
        }

        @Test
        void throwsBadRequest_whenFileIsEmpty() throws MinioException {
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file",
                    "avatar.jpg",
                    "image/jpeg",
                    "".getBytes());

            assertThatThrownBy(() -> photoService.uploadPhoto(USER_ID, emptyFile))
                    .isExactlyInstanceOf(BadRequestException.class)
                    .hasMessage("Файл с фото пуст");
            verify(photoRepository, never()).save(any(PhotoEntity.class));
            verify(minio, never()).putObject(any(PutObjectArgs.class));

        }

        @Test
        void throwsNotFoundExc_whenUserDoesNotExist() throws MinioException {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "avatar.jpg",
                    "image/jpeg",
                    "mock-data".getBytes());
            when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> photoService.uploadPhoto(USER_ID, file))
                    .isExactlyInstanceOf(NotFoundException.class)
                    .hasMessage("Пользователь не найден");
            verify(photoRepository, never()).save(any(PhotoEntity.class));
            verify(minio, never()).putObject(any(PutObjectArgs.class));
        }

        @Test
        void throwsPhotoExc_whenNumberOfPhotosGreaterThanLimit() throws MinioException {
            UserEntity user = readyUser();
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "avatar.jpg",
                    "image/jpeg",
                    "mock-data".getBytes());
            when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
            when(photoRepository.countByUserId(USER_ID)).thenReturn(MAX_PHOTOS);


            assertThatThrownBy(() -> photoService.uploadPhoto(USER_ID, file))
                    .isExactlyInstanceOf(PhotoException.class)
                    .hasMessage("Общее количество фотографий не может быть больше " + MAX_PHOTOS);
            verify(photoRepository, never()).save(any(PhotoEntity.class));
            verify(minio, never()).putObject(any(PutObjectArgs.class));
        }

        @Test
        void throwsPhotoExc_whenSizeOfPhotosGreaterThanLimit() throws MinioException {
            MockMultipartFile bigFile = new MockMultipartFile(
                    "file",
                    "avatar.jpg",
                    "image/jpeg",
                    new byte[Math.toIntExact(MAX_SIZE_BYTES) + 10]);
            stubUpload();


            assertThatThrownBy(() -> photoService.uploadPhoto(USER_ID, bigFile))
                    .isExactlyInstanceOf(PhotoException.class)
                    .hasMessage("Размер одной фотографии не должен превышать " + MAX_SIZE_BYTES / 1024 / 1024 + "МБ." +
                            "Размер предоставленной фотографии " + bigFile.getSize());
            verify(photoRepository, never()).save(any(PhotoEntity.class));
            verify(minio, never()).putObject(any(PutObjectArgs.class));
        }

        @Test
        void throwsPhotoExc_whenContentTypeWrong() throws MinioException {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "avatar.jpg",
                    "wrongType",
                    "mock-data".getBytes());
            stubUpload();


            assertThatThrownBy(() -> photoService.uploadPhoto(USER_ID, file))
                    .isExactlyInstanceOf(PhotoException.class)
                    .hasMessage("Только JPEG, PNG, WebP. Предоставлен " + file.getContentType());
            verify(photoRepository, never()).save(any(PhotoEntity.class));
            verify(minio, never()).putObject(any(PutObjectArgs.class));
        }

        @Test
        void throwsPhotoExc_whenMinioFailed() throws MinioException {
            stubUpload();
            when(minio.putObject(any(PutObjectArgs.class))).thenThrow(MinioException.class);

            assertThatThrownBy(() -> photoService.uploadPhoto(USER_ID, FILE))
                    .isExactlyInstanceOf(PhotoException.class)
                    .hasMessage("Failed to save photo");
            verify(photoRepository, never()).save(any(PhotoEntity.class));
        }

        @Test
        void doCleanup_whenSaveThrowsExc() throws MinioException {
            stubUpload();
            when(photoRepository.save(any(PhotoEntity.class))).thenThrow(DataIntegrityViolationException.class);

            assertThatThrownBy(() -> photoService.uploadPhoto(USER_ID, FILE))
                    .isExactlyInstanceOf(DataIntegrityViolationException.class);

            verify(minio).putObject(any(PutObjectArgs.class));
            verify(photoRepository).save(any(PhotoEntity.class));
            verify(minio).removeObject(any(RemoveObjectArgs.class));
        }

        @Test
        void throwsExc_whenSaveAndCleanupFailed() throws MinioException {
            stubUpload();
            when(photoRepository.save(any(PhotoEntity.class))).thenThrow(DataIntegrityViolationException.class);
            doThrow(new MinioException("cleanup fail"))
                    .when(minio).removeObject(any(RemoveObjectArgs.class));

            assertThatThrownBy(() -> photoService.uploadPhoto(USER_ID, FILE))
                    .isExactlyInstanceOf(DataIntegrityViolationException.class);
            verify(minio).putObject(any(PutObjectArgs.class));
            verify(minio).removeObject(any(RemoveObjectArgs.class));
            verify(photoRepository).save(any(PhotoEntity.class));
        }

        @Test
        void fillsEntityCorrectly_whenCorrectInput() throws MinioException {
            PhotoEntity saved = new PhotoEntity();
            saved.setId(42L);

            stubUpload();
            when(photoRepository.save(any(PhotoEntity.class))).thenReturn(saved);

            photoService.uploadPhoto(USER_ID, FILE);

            verify(minio).putObject(any(PutObjectArgs.class));
            verify(photoRepository).save(photoEntityCaptor.capture());
            PhotoEntity captured = photoEntityCaptor.getValue();
            assertThat(captured.isMain()).isTrue();
            assertThat(captured.getUserId()).isEqualTo(USER_ID);
            assertThat(captured.getSize()).isEqualTo(9);
            assertThat(captured.getObjectName()).endsWith(".jpg");
            assertThat(captured.getOriginalName()).isEqualTo("avatar.jpg");
            assertThat(captured.getContentType()).isEqualTo("image/jpeg");

        }

        @ParameterizedTest
        @CsvSource("""
                true, false, 2
                false, true, 0
                false, true, 3
                """)
        void fillsIsMainCorrectly_whenCorrectInput(boolean hasMain, boolean expected, int photoCount) throws MinioException {
            PhotoEntity saved = new PhotoEntity();
            saved.setId(42L);

            when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(readyUser()));
            when(photoRepository.countByUserId(USER_ID)).thenReturn(photoCount);
            when(photoRepository.existsByUserIdAndIsMainTrue(USER_ID)).thenReturn(hasMain);
            when(photoRepository.save(any(PhotoEntity.class))).thenReturn(saved);

            photoService.uploadPhoto(USER_ID, FILE);

            verify(minio).putObject(any(PutObjectArgs.class));
            verify(photoRepository).save(photoEntityCaptor.capture());
            PhotoEntity captured = photoEntityCaptor.getValue();
            assertThat(captured.isMain()).isEqualTo(expected);
        }
    }


    @Nested
    class GetSinglePhotoTest {

        @Test
        void returnsDto_whenPhotoExists() throws MinioException {
            Long photoId = 1L;
            PhotoEntity entity = photoEntity(photoId, USER_ID, "obj.jpg", false);
            PhotoDto expectedDto = mock(PhotoDto.class);
            String url = "http://presigned.url";

            when(photoRepository.findByIdOrThrow(photoId)).thenReturn(entity);
            when(minio.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenReturn(url);
            when(photoMapper.toDto(entity, url)).thenReturn(expectedDto);

            PhotoDto result = photoService.getSinglePhoto(photoId);

            assertThat(result).isEqualTo(expectedDto);
            verify(minio).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
            verify(photoMapper).toDto(entity, url);
        }

        @Test
        void throwsNotFound_whenPhotoDoesNotExist() {
            Long photoId = 1L;
            when(photoRepository.findByIdOrThrow(photoId))
                    .thenThrow(new NotFoundException("Фото не найдено", 404));

            assertThatThrownBy(() -> photoService.getSinglePhoto(photoId))
                    .isExactlyInstanceOf(NotFoundException.class)
                    .hasMessage("Фото не найдено");
        }
    }


    @Nested
    class GetSinglePhotoUrlTest {

        @Test
        void returnsUrl_whenPhotoExists() throws MinioException {
            Long photoId = 1L;
            PhotoEntity entity = photoEntity(photoId, USER_ID, "obj.jpg", false);
            String url = "http://presigned.url";

            when(photoRepository.findByIdOrThrow(photoId)).thenReturn(entity);
            when(minio.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenReturn(url);

            String result = photoService.getSinglePhotoUrl(photoId);

            assertThat(result).isEqualTo(url);
        }

        @Test
        void throwsNotFound_whenPhotoDoesNotExist() {
            Long photoId = 1L;
            when(photoRepository.findByIdOrThrow(photoId))
                    .thenThrow(new NotFoundException("Фото не найдено", 404));

            assertThatThrownBy(() -> photoService.getSinglePhotoUrl(photoId))
                    .isExactlyInstanceOf(NotFoundException.class);
        }
    }


    @Nested
    class GetListPhotosTest {

        @Test
        void returnsListOfDtos_whenPhotosExist() throws MinioException {
            Long userId = USER_ID;
            PhotoEntity p1 = photoEntity(1L, userId, "obj1.jpg", true);
            PhotoEntity p2 = photoEntity(2L, userId, "obj2.jpg", false);
            List<PhotoEntity> entities = List.of(p1, p2);
            String url1 = "url1";
            String url2 = "url2";
            PhotoDto dto1 = mock(PhotoDto.class);
            PhotoDto dto2 = mock(PhotoDto.class);

            when(photoRepository.findByUserIdOrderByUploadedAtDesc(userId)).thenReturn(entities);
            when(minio.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                    .thenReturn(url1)
                    .thenReturn(url2);
            when(photoMapper.toDto(p1, url1)).thenReturn(dto1);
            when(photoMapper.toDto(p2, url2)).thenReturn(dto2);

            List<PhotoDto> result = photoService.getListPhotos(userId);

            assertThat(result).containsExactly(dto1, dto2);
        }

        @Test
        void returnsEmptyList_whenNoPhotos() {
            Long userId = USER_ID;
            when(photoRepository.findByUserIdOrderByUploadedAtDesc(userId)).thenReturn(List.of());

            List<PhotoDto> result = photoService.getListPhotos(userId);

            assertThat(result).isEmpty();
        }

        @Test
        void returnsDtoWithNullUrl_whenMinioFails() throws MinioException {
            Long userId = USER_ID;
            PhotoEntity p1 = photoEntity(1L, userId, "obj1.jpg", true);
            List<PhotoEntity> entities = List.of(p1);
            PhotoDto dto1 = mock(PhotoDto.class);

            when(photoRepository.findByUserIdOrderByUploadedAtDesc(userId)).thenReturn(entities);
            when(minio.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                    .thenThrow(MinioException.class);
            when(photoMapper.toDto(p1, null)).thenReturn(dto1);

            List<PhotoDto> result = photoService.getListPhotos(userId);

            assertThat(result).containsExactly(dto1);
        }
    }


    @Nested
    class SetMainPhotoTest {

        @Test
        void setsMainPhoto_whenUserOwnsPhoto() {
            Long curUserId = USER_ID;
            Long photoId = 1L;
            PhotoEntity photo = photoEntity(photoId, curUserId, "obj.jpg", false);
            when(photoRepository.findById(photoId)).thenReturn(Optional.of(photo));

            photoService.setMainPhoto(curUserId, photoId);

            verify(photoRepository).setNewMainPhoto(curUserId, photoId);
        }

        @Test
        void throwsNotFound_whenPhotoDoesNotExist() {
            Long photoId = 1L;
            when(photoRepository.findById(photoId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> photoService.setMainPhoto(USER_ID, photoId))
                    .isExactlyInstanceOf(NotFoundException.class)
                    .hasMessage("Фото с таким id не существует");
        }

        @Test
        void throwsForbidden_whenUserDoesNotOwnPhoto() {
            Long otherUserId = 999L;
            Long photoId = 1L;
            PhotoEntity photo = photoEntity(photoId, otherUserId, "obj.jpg", false);
            when(photoRepository.findById(photoId)).thenReturn(Optional.of(photo));

            assertThatThrownBy(() -> photoService.setMainPhoto(USER_ID, photoId))
                    .isExactlyInstanceOf(ForbiddenException.class)
                    .hasMessage("Вы не можете управлять чужим фото");
        }
    }


    @Nested
    class DeletePhotoTest {

        @Test
        void deletesPhoto_whenPhotoNotMain() throws MinioException {
            Long curUserId = USER_ID;
            Long photoId = 1L;
            UserEntity user = readyUser();
            PhotoEntity photo = photoEntity(photoId, curUserId, "obj.jpg", false);

            when(userRepository.findByIdForUpdate(curUserId)).thenReturn(Optional.of(user));
            when(photoRepository.findById(photoId)).thenReturn(Optional.of(photo));

            photoService.deletePhoto(curUserId, photoId);

            verify(photoRepository).delete(photo);
            verify(minio).removeObject(any(RemoveObjectArgs.class));
            verify(registry).counter("photos.deleted.total");
        }

        @Test
        void setsNewMain_whenDeletedPhotoWasMainAndOtherPhotosExist() throws MinioException {
            Long curUserId = USER_ID;
            Long photoId = 1L;
            UserEntity user = readyUser();
            PhotoEntity photo = photoEntity(photoId, curUserId, "obj.jpg", true);
            PhotoEntity otherPhoto = photoEntity(2L, curUserId, "other.jpg", false);

            when(userRepository.findByIdForUpdate(curUserId)).thenReturn(Optional.of(user));
            when(photoRepository.findById(photoId)).thenReturn(Optional.of(photo));
            when(photoRepository.findByUserIdOrderByUploadedAtDesc(curUserId))
                    .thenReturn(List.of(otherPhoto));

            photoService.deletePhoto(curUserId, photoId);

            assertThat(otherPhoto.isMain()).isTrue();
            verify(photoRepository).delete(photo);
            verify(minio).removeObject(any(RemoveObjectArgs.class));
        }

        @Test
        void setsReadyForDeckFalse_whenDeletedPhotoWasMainAndNoOtherPhotos() throws MinioException {
            Long curUserId = USER_ID;
            Long photoId = 1L;
            UserEntity user = readyUser();
            user.setReadyForDeck(true);
            PhotoEntity photo = photoEntity(photoId, curUserId, "obj.jpg", true);

            when(userRepository.findByIdForUpdate(curUserId)).thenReturn(Optional.of(user));
            when(photoRepository.findById(photoId)).thenReturn(Optional.of(photo));
            when(photoRepository.findByUserIdOrderByUploadedAtDesc(curUserId)).thenReturn(List.of());

            photoService.deletePhoto(curUserId, photoId);

            assertThat(user.getReadyForDeck()).isFalse();
            verify(photoRepository).delete(photo);
            verify(minio).removeObject(any(RemoveObjectArgs.class));
        }

        @Test
        void throwsNotFound_whenUserDoesNotExist() {
            Long curUserId = USER_ID;
            Long photoId = 1L;
            when(userRepository.findByIdForUpdate(curUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> photoService.deletePhoto(curUserId, photoId))
                    .isExactlyInstanceOf(NotFoundException.class)
                    .hasMessage("Пользователь не найден");
        }

        @Test
        void throwsNotFound_whenPhotoDoesNotExist() {
            Long curUserId = USER_ID;
            Long photoId = 1L;
            UserEntity user = readyUser();
            when(userRepository.findByIdForUpdate(curUserId)).thenReturn(Optional.of(user));
            when(photoRepository.findById(photoId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> photoService.deletePhoto(curUserId, photoId))
                    .isExactlyInstanceOf(NotFoundException.class)
                    .hasMessage("Фото с таким id не существует");
        }

        @Test
        void throwsForbidden_whenUserDoesNotOwnPhoto() {
            Long curUserId = USER_ID;
            Long otherUserId = 999L;
            Long photoId = 1L;
            UserEntity user = readyUser();
            PhotoEntity photo = photoEntity(photoId, otherUserId, "obj.jpg", false);

            when(userRepository.findByIdForUpdate(curUserId)).thenReturn(Optional.of(user));
            when(photoRepository.findById(photoId)).thenReturn(Optional.of(photo));

            assertThatThrownBy(() -> photoService.deletePhoto(curUserId, photoId))
                    .isExactlyInstanceOf(ForbiddenException.class)
                    .hasMessage("Вы не можете удалить чужое фото");
        }

        @Test
        void throwsMinioException_whenMinioRemoveFails() throws MinioException {
            Long curUserId = USER_ID;
            Long photoId = 1L;
            UserEntity user = readyUser();
            PhotoEntity photo = photoEntity(photoId, curUserId, "obj.jpg", false);

            when(userRepository.findByIdForUpdate(curUserId)).thenReturn(Optional.of(user));
            when(photoRepository.findById(photoId)).thenReturn(Optional.of(photo));
            doThrow(new MinioException("Minio error")).when(minio).removeObject(any(RemoveObjectArgs.class));

            assertThatThrownBy(() -> photoService.deletePhoto(curUserId, photoId))
                    .isExactlyInstanceOf(MinioException.class)
                    .hasMessage("Minio error");
        }
    }


    @Nested
    class HasMainPhotoTest {

        @Test
        void returnsTrue_whenMainPhotoExists() {
            when(photoRepository.existsByUserIdAndIsMainTrue(USER_ID)).thenReturn(true);
            assertThat(photoService.hasMainPhoto(USER_ID)).isTrue();
        }

        @Test
        void returnsFalse_whenNoMainPhoto() {
            when(photoRepository.existsByUserIdAndIsMainTrue(USER_ID)).thenReturn(false);
            assertThat(photoService.hasMainPhoto(USER_ID)).isFalse();
        }
    }


    @Nested
    class GetMainPhotoUrlsTest {

        @Test
        void returnsMapOfUrls_whenUserIdsProvided() throws MinioException {
            Collection<Long> userIds = List.of(1L, 2L);
            PhotoEntity p1 = photoEntity(1L, 1L, "obj1.jpg", true);
            PhotoEntity p2 = photoEntity(2L, 2L, "obj2.jpg", true);
            String url1 = "url1";
            String url2 = "url2";

            when(photoRepository.findMainPhotosByUserIds(userIds)).thenReturn(List.of(p1, p2));
            when(minio.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                    .thenReturn(url1)
                    .thenReturn(url2);

            Map<Long, String> result = photoService.getMainPhotoUrls(userIds);

            assertThat(result).containsEntry(1L, url1).containsEntry(2L, url2);
        }

        @Test
        void returnsEmptyMap_whenUserIdsIsEmpty() {
            assertThat(photoService.getMainPhotoUrls(Collections.emptyList())).isEmpty();
        }

        @Test
        void returnsEmptyMap_whenUserIdsIsNull() {
            assertThat(photoService.getMainPhotoUrls(null)).isEmpty();
        }
    }


    @Nested
    class GetMainPhotoTest {

        @Test
        void returnsDto_whenMainPhotoExists() throws MinioException {
            Long userId = USER_ID;
            PhotoEntity photo = photoEntity(1L, userId, "obj.jpg", true);
            String url = "http://presigned.url";
            PhotoDto expectedDto = mock(PhotoDto.class);

            when(photoRepository.findMain(userId)).thenReturn(Optional.of(photo));
            when(minio.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenReturn(url);
            when(photoMapper.toDto(photo, url)).thenReturn(expectedDto);

            PhotoDto result = photoService.getMainPhoto(userId);

            assertThat(result).isEqualTo(expectedDto);
        }

        @Test
        void throwsNotFound_whenMainPhotoDoesNotExist() {
            Long userId = USER_ID;
            when(photoRepository.findMain(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> photoService.getMainPhoto(userId))
                    .isExactlyInstanceOf(NotFoundException.class)
                    .hasMessage("Главное фото не найдено");
        }
    }
}