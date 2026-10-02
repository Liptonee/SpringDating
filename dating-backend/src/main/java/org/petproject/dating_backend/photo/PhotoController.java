package org.petproject.dating_backend.photo;

import io.minio.errors.MinioException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.petproject.dating_backend.common.exception.ErrorDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api")
@RestController
@Tag(name = "Фото", description = "Загрузка, просмотр и удаление фотографий")
public class PhotoController {


    private final PhotoService photoService;


    @Operation(
            summary = "Загружает фото в систему.",
            description = """
                    Принимает фото размером до 5МБ в форматах JPEG, PNG, WebP.
                    Возвращает id фото.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "201", description = "Фото успешно загружено.",
            content = @Content(schema = @Schema(implementation = Long.class, example = "42")))
    @ApiResponse(responseCode = "400", description = """
            Общее количество фото пользователя больше лимита,
            или размер фото больше лимита,
            или формат фото не поддерживается.
            """,
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    @ApiResponse(responseCode = "500", description = "Ошибка при загрузке в хранилище.",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    @PostMapping("/photos")
    public ResponseEntity<Long> uploadPhoto(
            @AuthenticationPrincipal Long curUserId,
            @RequestParam("file") MultipartFile file
    ) throws MinioException {
        return ResponseEntity.status(201)
                .body(photoService.uploadPhoto(curUserId, file));
    }


    @Operation(summary = "Возвращает запрашиваемое фото по id",
            description = "Возвращает метаданные о запрашиваемом фото + временную ссылку на само фото.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Успех",
            content = @Content(schema = @Schema(implementation = PhotoDto.class)))
    @GetMapping("/photos/{photoId}")
    public ResponseEntity<PhotoDto> getSinglePhoto(
            @PathVariable Long photoId
    ) throws MinioException {
        return ResponseEntity.ok().body(photoService.getSinglePhoto(photoId));
    }

    @Operation(summary = "Возвращает временный url запрашиваемого фото по id",
            description = "Возвращает временный url запрашиваемого фото по id.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Успех",
            content = @Content(schema = @Schema(implementation = String.class)))
    @GetMapping("/photos/{photoId}/url")
    public ResponseEntity<String> getSinglePhotoUrl(
            @PathVariable Long photoId
    ) throws MinioException {
        return ResponseEntity.ok().body(photoService.getSinglePhotoUrl(photoId));
    }

    @Operation(summary = "Возвращает все фото запрашиваемого пользователя",
            description = """
                    Возвращает метаданные обо всех фото запрашиваемого пользователя
                    + временные ссылки на сами фото.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Успех",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = PhotoDto.class))))
    @GetMapping("users/{userId}/photos")
    public ResponseEntity<List<PhotoDto>> getListPhotos(
            @PathVariable() Long userId
    ) {
        return ResponseEntity.ok(photoService.getListPhotos(userId));
    }

    @Operation(summary = "Удаляет запрашиваемое фото у текущего пользователя",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "204", description = "Фото успешно удалено")
    @DeleteMapping("/{photoId}")
    public ResponseEntity<Void> deletePhoto(
            @AuthenticationPrincipal Long curUserId,
            @PathVariable Long photoId
    ) throws MinioException {
        photoService.deletePhoto(curUserId, photoId);
        return ResponseEntity.noContent().build();
    }


}
