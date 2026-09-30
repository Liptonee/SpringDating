package org.petproject.dating_backend.photo;

import io.minio.*;
import io.minio.errors.MinioException;
import lombok.extern.slf4j.Slf4j;
import org.petproject.dating_backend.common.exception.ForbiddenException;
import org.petproject.dating_backend.common.exception.NotFoundException;
import org.petproject.dating_backend.common.exception.PhotoException;
import org.petproject.dating_backend.user.UserEntity;
import org.petproject.dating_backend.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PhotoService {


    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_PHOTOS = 5;
    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024L; // 5 MB
    private static final String CACHE_NAME = "photos";
    private final String BUCKET_NAME;

    private final PhotoRepository photoRepository;
    private final UserRepository userRepository;
    private final MinioClient minio;
    private final PhotoMapper photoMapper;


    public PhotoService(@Value("${minio.bucket-name}") String bucketName,
                        PhotoRepository photoRepository,
                        UserRepository userRepository,
                        MinioClient minio,
                        PhotoMapper photoMapper) {
        this.BUCKET_NAME = bucketName;
        this.photoRepository = photoRepository;
        this.userRepository = userRepository;
        this.minio = minio;
        this.photoMapper = photoMapper;
    }


    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "'list:' + #curUserId")
    public Long uploadPhoto(Long curUserId, MultipartFile file) throws MinioException {
        Long size = file.getSize();
        String contentType = file.getContentType() == null
                ? "image/png"
                : file.getContentType();

        //Блокировка строки, чтобы избежать race condition при проверке количества фото
        userRepository.lockById(curUserId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден", 404));
        validate(file, curUserId, size, contentType);

        String originalName = file.getOriginalFilename() == null
                ? "untitled"
                : file.getOriginalFilename();
        String extension = switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
        String objectName = UUID.randomUUID() + extension;

        PhotoEntity photoEntity = new PhotoEntity();
        photoEntity.setUser(user);
        photoEntity.setObjectName(objectName);
        photoEntity.setOriginalName(originalName);
        photoEntity.setContentType(contentType);
        photoEntity.setSize(size);

        try (InputStream inputStream = file.getInputStream()) {
            minio.putObject(
                    PutObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1L)
                            .contentType(file.getContentType())
                            .build()

            );
        } catch (IOException e) {
            throw new PhotoException("Failed to save photo", e, 500);
        }

        try {
            return photoRepository.save(photoEntity).getId();
        } catch (Exception e) {
            try {
                minio.removeObject(RemoveObjectArgs.builder()
                        .bucket(BUCKET_NAME).object(objectName).build());
            } catch (Exception cleanup) {
                log.error("Cleanup failed for {}", objectName, cleanup);
            }
            throw e;
        }

    }

    private void validate(
            MultipartFile file,
            Long curUserId,
            Long size,
            String contentType
    ) throws PhotoException {
        if (photoRepository.countByUserId(curUserId) >= MAX_PHOTOS) {
            throw new PhotoException("Общее количество фотографий не может быть больше " + MAX_PHOTOS, 400);
        }
        if (size > MAX_SIZE_BYTES) {
            throw new PhotoException("Размер одной фотографии не должен превышать " + MAX_SIZE_BYTES / 1024 / 1024 + "МБ." +
                    "Размер предоставленной фотографии " + file.getSize(),
                    400);
        }
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new PhotoException("Только JPEG, PNG, WebP. Предоставлен " + file.getContentType(), 400);
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_NAME, key = "'single:dto'+ #photoId")
    public PhotoDto getSinglePhoto(Long photoId) throws MinioException {
        PhotoEntity photoEntity = photoRepository.findByIdOrThrow(photoId);

        String url = minio.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Http.Method.GET)
                        .bucket(BUCKET_NAME)
                        .object(photoEntity.getObjectName())
                        .expiry(6, TimeUnit.HOURS)
                        .build()
        );

        return photoMapper.toDto(photoEntity, url);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_NAME, key = "'list:' + #userId")
    public List<PhotoDto> getListPhotos(Long userId) {
        return photoRepository.findByUserIdOrderByUploadedAtDesc(userId)
                .stream()
                .map(this::toDtoWithUrl)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private PhotoDto toDtoWithUrl(PhotoEntity entity) {
        String url = generatePresignedUrl(entity.getObjectName());
        return photoMapper.toDto(entity, url);
    }

    private String generatePresignedUrl(String objectName) {
        try {
            return minio.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Http.Method.GET)
                            .bucket(BUCKET_NAME)
                            .object(objectName)
                            .expiry(6, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for {}", objectName, e);
            return null;
        }
    }

    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = CACHE_NAME, key = "'list:' + #curUserId"),
                    @CacheEvict(value = CACHE_NAME, key = "'single:dto' + #photoId"),
                    @CacheEvict(value = CACHE_NAME, key = "'single:url' + #photoId")
            }
    )
    public void deletePhoto(Long curUserId, Long photoId) throws MinioException {
        PhotoEntity photo = photoRepository.findById(photoId).orElse(null);

        if (photo == null) return;

        if (!photo.getUser().getId().equals(curUserId)) {
            throw new ForbiddenException("Вы не можете удалить чужое фото", 403);
        }

        photoRepository.delete(photo);

        try {
            minio.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(photo.getObjectName())
                            .build()
            );
        } catch (MinioException e) {
            log.error("The photo could not be deleted from MiniO, but the photo was deleted from the database.");
            throw e;
        }
    }
}
