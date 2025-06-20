package com.diplom.agafonov.service;

import com.diplom.agafonov.controller.dto.UserEditRequest;
import com.diplom.agafonov.entity.Role;
import com.diplom.agafonov.entity.User;
import com.diplom.agafonov.repository.RoleRepository;
import com.diplom.agafonov.repository.UserRepository;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() throws Exception {
        java.lang.reflect.Field jwtSecretField = AuthService.class.getDeclaredField("jwtSecret");
        jwtSecretField.setAccessible(true);
        String secureKey = Base64.getEncoder().encodeToString(Keys.secretKeyFor(SignatureAlgorithm.HS512).getEncoded());
        jwtSecretField.set(authService, secureKey);

        java.lang.reflect.Field jwtExpirationMsField = AuthService.class.getDeclaredField("jwtExpirationMs");
        jwtExpirationMsField.setAccessible(true);
        jwtExpirationMsField.set(authService, 3600000L);
    }

    @Test
    void register_success() {
        when(userRepository.findByLogin("testuser")).thenReturn(Optional.empty());
        Role role = new Role();
        role.setName("ROLE_USER");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        User user = authService.register("testuser", "password", "test@example.com",
                "Doe", "John", "Middle", new Date());
        assertEquals("testuser", user.getLogin());
        assertEquals("encoded-password", user.getPassword());
        assertEquals("test@example.com", user.getEmail());
        assertTrue(user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_USER")));
        assertNotNull(user.getDateOfBirth());
    }
    @Test
    void register_userExists() {
        when(userRepository.findByLogin("testuser")).thenReturn(Optional.of(new User()));
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.register("testuser", "password", "test@example.com", "Doe", "John", "Middle", new Date());
        });
        assertEquals("Пользователь с логином testuser уже существует", exception.getMessage());
    }

    @Test
    void login_success() {
        User user = new User();
        user.setLogin("testuser");
        user.setPassword("encoded-password");
        Set<Role> roles = new HashSet<>();
        Role role = new Role();
        role.setName("ROLE_USER");
        roles.add(role);
        user.setRoles(roles);
        when(userRepository.findByLogin("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);
        String token = authService.login("testuser", "password");
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);
    }
    @Test
    void login_invalidPassword() {
        User user = new User();
        user.setLogin("testuser");
        user.setPassword("encoded-password");
        when(userRepository.findByLogin("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "encoded-password")).thenReturn(false);
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login("testuser", "wrongpassword");
        });
        assertEquals("Неверный пароль", exception.getMessage());
    }

    @Test
    void editUser_success_noLoginChange() {
        User existingUser = new User();
        existingUser.setLogin("testuser");
        existingUser.setEmail("old@example.com");
        existingUser.setFirstName("Old");
        existingUser.setLastName("User");
        UserEditRequest editRequest = new UserEditRequest();
        editRequest.setEmail("new@example.com");
        editRequest.setFirstName("New");
        editRequest.setLastName("User");
        editRequest.setMiddleName("Middle");
        editRequest.setLogin("testuser"); // Логин не меняется
        when(userRepository.findByLogin("testuser")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        String result = authService.editUser("testuser", editRequest);
        assertNull(result); // Ожидаем null, так как логин не изменился
        assertEquals("new@example.com", existingUser.getEmail());
        assertEquals("New", existingUser.getFirstName());
    }
    @Test
    void editUser_success_withLoginChange() {
        User existingUser = new User();
        existingUser.setLogin("testuser");
        existingUser.setEmail("old@example.com");
        existingUser.setFirstName("Old");
        existingUser.setLastName("User");
        UserEditRequest editRequest = new UserEditRequest();
        editRequest.setEmail("new@example.com");
        editRequest.setFirstName("New");
        editRequest.setLastName("User");
        editRequest.setMiddleName("Middle");
        editRequest.setLogin("newtestuser"); // Логин меняется
        when(userRepository.findByLogin("testuser")).thenReturn(Optional.of(existingUser));
        when(userRepository.findByLogin("newtestuser")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        String result = authService.editUser("testuser", editRequest);
        assertNotNull(result); // Ожидаем токен, так как логин изменился
        assertTrue(result.split("\\.").length == 3); // Проверяем, что это JWT токен
        assertEquals("new@example.com", existingUser.getEmail());
        assertEquals("New", existingUser.getFirstName());
        assertEquals("newtestuser", existingUser.getLogin());
    }

    @Test
    void editUser_userNotFound() {
        UserEditRequest editRequest = new UserEditRequest();
        editRequest.setEmail("new@example.com");
        editRequest.setFirstName("New");
        editRequest.setLastName("User");
        editRequest.setMiddleName("Middle");
        when(userRepository.findByLogin("testuser")).thenReturn(Optional.empty());
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.editUser("testuser", editRequest);
        });
        assertEquals("Пользователь с логином testuser не найден", exception.getMessage());
    }

    @Test
    void changePassword_userNotFound() {
        when(userRepository.findByLogin("testuser")).thenReturn(Optional.empty());
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.changePassword("testuser", "oldpassword", "newpassword");
        });
        assertEquals("Пользователь с логином testuser не найден", exception.getMessage());
    }

    @Test
    void changePassword_success() {
        User existingUser = new User();
        existingUser.setLogin("testuser");
        existingUser.setPassword("encoded-old-password");
        when(userRepository.findByLogin("testuser")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("oldpassword", "encoded-old-password")).thenReturn(true);
        when(passwordEncoder.encode("newpassword")).thenReturn("encoded-new-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        authService.changePassword("testuser", "oldpassword", "newpassword");
        verify(passwordEncoder).matches("oldpassword", "encoded-old-password");
        verify(passwordEncoder).encode("newpassword");
        verify(userRepository).save(existingUser);
        assertEquals("encoded-new-password", existingUser.getPassword());
    }

    @Test
    void changePassword_invalidOldPassword() {
        User existingUser = new User();
        existingUser.setLogin("testuser");
        existingUser.setPassword("encoded-old-password");
        when(userRepository.findByLogin("testuser")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrongpassword", "encoded-old-password")).thenReturn(false);
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.changePassword("testuser", "wrongpassword", "newpassword");
        });
        assertEquals("Неверный текущий пароль", exception.getMessage());
    }
}