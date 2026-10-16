package org.petproject.dating_backend.photo;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PhotoMapperTest {

    private final PhotoMapper mapper = new PhotoMapperImpl();

    private static final Long ID = 42L;
    private static final Long USER_ID = 123L;
    private static final String OBJECT_NAME = "a1b2c3d4.jpg";
    private static final String ORIGINAL_NAME = "cat.jpg";
    private static final String CONTENT_TYPE = "image/jpeg";
    private static final Long SIZE = 245123L;
    private static final Instant UPLOADED_AT = Instant.parse("2026-09-25T10:15:30Z");
    private static final String URL = "http://localhost:9000/photos/a1b2c3d4.jpg?X-Amz-Signature=...";

    @Test
    void toDto_mapsAllFieldsCorrectly_whenIsMainTrue() {
        PhotoEntity entity = new PhotoEntity();
        entity.setId(ID);
        entity.setUserId(USER_ID);
        entity.setObjectName(OBJECT_NAME);
        entity.setOriginalName(ORIGINAL_NAME);
        entity.setContentType(CONTENT_TYPE);
        entity.setSize(SIZE);
        entity.setMain(true);
        entity.setUploadedAt(UPLOADED_AT);

        PhotoDto dto = mapper.toDto(entity, URL);

        assertThat(dto.id()).isEqualTo(ID);
        assertThat(dto.userId()).isEqualTo(USER_ID);
        assertThat(dto.originalName()).isEqualTo(ORIGINAL_NAME);
        assertThat(dto.contentType()).isEqualTo(CONTENT_TYPE);
        assertThat(dto.size()).isEqualTo(SIZE);
        assertThat(dto.isMain()).isTrue();
        assertThat(dto.url()).isEqualTo(URL);
        assertThat(dto.uploadedAt()).isEqualTo(UPLOADED_AT);
    }

    @Test
    void toDto_mapsAllFieldsCorrectly_whenIsMainFalseAndUrlNull() {
        PhotoEntity entity = new PhotoEntity();
        entity.setId(ID);
        entity.setUserId(USER_ID);
        entity.setObjectName(OBJECT_NAME);
        entity.setOriginalName(ORIGINAL_NAME);
        entity.setContentType(CONTENT_TYPE);
        entity.setSize(SIZE);
        entity.setMain(false);
        entity.setUploadedAt(UPLOADED_AT);

        PhotoDto dto = mapper.toDto(entity, null);

        assertThat(dto.id()).isEqualTo(ID);
        assertThat(dto.userId()).isEqualTo(USER_ID);
        assertThat(dto.originalName()).isEqualTo(ORIGINAL_NAME);
        assertThat(dto.contentType()).isEqualTo(CONTENT_TYPE);
        assertThat(dto.size()).isEqualTo(SIZE);
        assertThat(dto.isMain()).isFalse();
        assertThat(dto.url()).isNull();
        assertThat(dto.uploadedAt()).isEqualTo(UPLOADED_AT);
    }
}