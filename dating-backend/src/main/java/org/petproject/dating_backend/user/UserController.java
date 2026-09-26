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
            content = @Content(schema = @Schema(implementation = UserDto.class)))
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(
            @AuthenticationPrincipal Long curUserId
    ) {
        return ResponseEntity.ok(userService.getCurrentUser(curUserId));
    }


    @Operation(summary = "Возвращает запрашиваемого пользователя",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Успех",
            content = @Content(schema = @Schema(implementation = UserDto.class)))
    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUser(
            @AuthenticationPrincipal Long curUserId,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(userService.getUser(curUserId, userId));
    }

    @Operation(summary = "Изменяет данные пользователя",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Успех",
            content = @Content(schema = @Schema(implementation = UserDto.class)))
    @PatchMapping("/me")
    public ResponseEntity<UserDto> patchCurrentUser(
            @AuthenticationPrincipal Long curUserId,
            @RequestBody @Valid UserDto patchUserDto
    ) {
        return ResponseEntity.ok(userService.patchCurrentUser(curUserId, patchUserDto));
    }


}
