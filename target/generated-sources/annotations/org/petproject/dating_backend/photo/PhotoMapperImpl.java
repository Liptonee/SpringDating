package org.petproject.dating_backend.photo;

import java.time.Instant;
import javax.annotation.processing.Generated;
import org.petproject.dating_backend.user.UserEntity;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-26T14:57:24+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 26.0.1 (Oracle Corporation)"
)
@Component
public class PhotoMapperImpl implements PhotoMapper {

    @Override
    public PhotoDto toDto(PhotoEntity entity, String url) {
        if ( entity == null && url == null ) {
            return null;
        }

        Long userId = null;
        Long id = null;
        String originalName = null;
        String contentType = null;
        Long size = null;
        Instant uploadedAt = null;
        if ( entity != null ) {
            userId = entityUserId( entity );
            id = entity.getId();
            originalName = entity.getOriginalName();
            contentType = entity.getContentType();
            size = entity.getSize();
            uploadedAt = entity.getUploadedAt();
        }
        String url1 = null;
        url1 = url;

        PhotoDto photoDto = new PhotoDto( id, userId, originalName, contentType, size, url1, uploadedAt );

        return photoDto;
    }

    private Long entityUserId(PhotoEntity photoEntity) {
        UserEntity user = photoEntity.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getId();
    }
}
