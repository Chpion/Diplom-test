package com.diplom.agafonov.controller;

import com.diplom.agafonov.controller.dto.LoginRequest;
import com.diplom.agafonov.controller.dto.UserResponse;
import com.diplom.agafonov.controller.dto.UserEditRequest;
import com.diplom.agafonov.controller.dto.PasswordChangeRequest;
import com.diplom.agafonov.entity.User;
import com.diplom.agafonov.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Аутентификация", description = "API для регистрации, входа и управления пользователями")
@CrossOrigin(origins = {"http://localhost:9000", "http://localhost:8080", "http://localhost:3000"})
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Регистрация нового пользователя", description = "Создает нового пользователя с ролью USER")
    @PostMapping("/register")
    public ResponseEntity<String> register(
            @RequestParam String login,
            @RequestParam String password,
            @RequestParam String email,
            @RequestParam String lastname,
            @RequestParam String firstname,
            @RequestParam String middlename,
            @RequestParam String dateOfBirth) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd");
            Date birthDate = dateFormat.parse(dateOfBirth);
            authService.register(login, password, email, lastname, firstname, middlename, birthDate);
            return ResponseEntity.ok("Пользователь успешно зарегистрирован");
        } catch (ParseException e) {
            return ResponseEntity.badRequest().body("Ошибка парсинга даты: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка регистрации: " + e.getMessage());
        }
    }

    @Operation(summary = "Вход пользователя", description = "Аутентифицирует пользователя и возвращает JWT токен")
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request) {
        try {
            System.out.println("Получен запрос на вход: login=" + request.getLogin());
            String token = authService.login(request.getLogin(), request.getPassword());
            return ResponseEntity.ok(token);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Ошибка входа: " + e.getMessage() + " | Причина: " + (e.getCause() != null ? e.getCause().getMessage() : "Нет причины"));
        }
    }

    @Operation(summary = "Удаление пользователя", description = "Удаляет пользователя по логину (только для администратора)")
    @DeleteMapping("/users/{login}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteUser(@PathVariable String login) {
        try {
            authService.deleteUser(login);
            return ResponseEntity.ok("Пользователь с логином " + login + " успешно удален");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Ошибка удаления пользователя: " + e.getMessage());
        }
    }

    @Operation(summary = "Изменение роли пользователя", description = "Обновляет роль пользователя (только для администратора, только одна роль)")
    @PutMapping("/users/{login}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateUserRoles(@PathVariable String login, @RequestBody String roleName) {
        try {
            System.out.println("Received roleName: " + roleName); // Добавим логирование
            authService.updateUserRoles(login, roleName);
            return ResponseEntity.ok("Роль пользователя с логином " + login + " успешно обновлена");
        } catch (Exception e) {
            System.out.println("Ошибка обновления роли: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Ошибка обновления роли: " + e.getMessage());
        }
    }

    @Operation(summary = "Получение всех пользователей", description = "Возвращает список всех пользователей (только для администратора)")
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        try {
            List<UserResponse> users = authService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @Operation(summary = "Поиск пользователей", description = "Ищет пользователей по логину, имени, фамилии или email (только для администратора)")
    @GetMapping("/users/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> searchUsers(@RequestParam String keyword) {
        try {
            List<UserResponse> users = authService.searchUsers(keyword);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @Operation(summary = "Редактирование данных пользователя", description = "Позволяет пользователю изменить свои данные")
    @PutMapping("/edit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> editUser(@RequestBody UserEditRequest request, Authentication authentication) {
        try {
            String currentLogin = authentication.getName();
            System.out.println("Редактирование пользователя: " + currentLogin);
            String newToken = authService.editUser(currentLogin, request);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Данные пользователя успешно обновлены");
            if (newToken != null) {
                response.put("token", newToken);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("Ошибка редактирования: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Ошибка редактирования данных: " + e.getMessage()));
        }
    }

    @Operation(summary = "Смена пароля", description = "Позволяет пользователю сменить пароль, указав текущий пароль")
    @PutMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> changePassword(@RequestBody PasswordChangeRequest request, Authentication authentication) {
        try {
            String login = authentication.getName();
            authService.changePassword(login, request.getCurrentPassword(), request.getNewPassword());
            return ResponseEntity.ok("Пароль успешно изменен");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Ошибка смены пароля: " + e.getMessage());
        }
    }

    @Operation(summary = "Получение текущего пользователя", description = "Возвращает данные текущего аутентифицированного пользователя")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        try {
            System.out.println("Запрос к /api/auth/me, authentication: " + authentication);
            if (authentication == null || authentication.getName() == null) {
                System.out.println("Ошибка: authentication или login равен null");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }
            String login = authentication.getName();
            System.out.println("Получение пользователя с логином: " + login);
            UserResponse user = authService.getUserByLogin(login);
            if (user == null) {
                System.out.println("Пользователь не найден для логина: " + login);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            System.out.println("Ошибка в getCurrentUser: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}