package org.petproject.dating_backend.user;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Контроллер пользователей",
        description = "Просмотр пользователей, а также редактирование информации текущего пользователя")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Возвращает текущего пользователя",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Успех",
            content = @Content(schema = @Schema(implementation = ProfileUserDto.class)))
    @GetMapping("/me")
    public ResponseEntity<ProfileUserDto> getCurrentUser(
            @AuthenticationPrincipal Long curUserId
    ) {
        return ResponseEntity.ok(userService.getCurrentUser(curUserId));
    }


    @Operation(summary = "Возвращает запрашиваемого пользователя",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Успех",
            content = @Content(schema = @Schema(implementation = GetUserDto.class)))
    @GetMapping("/{userId}")
    public ResponseEntity<GetUserDto> getUser(
            @AuthenticationPrincipal Long curUserId,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(userService.getUser(curUserId, userId));
    }

    @Operation(summary = "Изменяет данные пользователя",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Успех",
            content = @Content(schema = @Schema(implementation = ProfileUserDto.class)))
    @PatchMapping("/me")
    public ResponseEntity<ProfileUserDto> patchCurrentUser(
            @AuthenticationPrincipal Long curUserId,
            @RequestBody @Valid ProfileUserDto patchProfileUserDto
    ) {
        return ResponseEntity.ok(userService.patchCurrentUser(curUserId, patchProfileUserDto));
    }


}
