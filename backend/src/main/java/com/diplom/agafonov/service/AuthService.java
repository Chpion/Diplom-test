package com.diplom.agafonov.service;

import com.diplom.agafonov.controller.dto.UserResponse;
import com.diplom.agafonov.controller.dto.UserEditRequest;
import com.diplom.agafonov.entity.Role;
import com.diplom.agafonov.entity.User;
import com.diplom.agafonov.repository.RoleRepository;
import com.diplom.agafonov.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Autowired
    public AuthService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(String login, String password, String email, String lastName, String firstName, String middleName, Date dateOfBirth) {
        if (userRepository.findByLogin(login).isPresent()) {
            throw new RuntimeException("Пользователь с логином " + login + " уже существует");
        }

        User user = new User();
        user.setLogin(login);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setLastName(lastName);
        user.setFirstName(firstName);
        user.setMiddleName(middleName);
        user.setDateOfBirth(dateOfBirth);

        Set<Role> roles = new HashSet<>();
        Role role = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Роль USER не найдена"));
        roles.add(role);
        user.setRoles(roles);

        return userRepository.save(user);
    }

    public String login(String login, String password) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Неверный пароль");
        }

        return generateJwtToken(user);
    }

    public User addSearchQuery(String login, String query) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        user.getSearchQueries().add(query);
        return userRepository.save(user);
    }

    public void deleteUser(String login) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new RuntimeException("Пользователь с логином " + login + " не найден"));
        userRepository.delete(user);
    }

    public void updateUserRoles(String login, String roleName) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new RuntimeException("Пользователь с логином " + login + " не найден"));

        String cleanedRoleName = roleName.replaceAll("^\"|\"$", "").toUpperCase();
        if (!cleanedRoleName.startsWith("ROLE_")) {
            cleanedRoleName = "ROLE_" + cleanedRoleName;
        }
        System.out.println("Attempting to set role: " + cleanedRoleName + " for user: " + login);

        String finalCleanedRoleName = cleanedRoleName;
        Role role = roleRepository.findByName(cleanedRoleName)
                .orElseThrow(() -> new RuntimeException("Роль " + finalCleanedRoleName + " не найдена"));

        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);

        userRepository.save(user);
        System.out.println("Role updated successfully for user: " + login);
    }

    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> searchUsers(String keyword) {
        String searchPattern = "%" + keyword.toLowerCase() + "%";
        List<User> users = userRepository.findByLoginLikeOrEmailLikeOrFirstNameLikeOrLastNameLike(
                searchPattern);
        return users.stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    public String editUser(String login, UserEditRequest request) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new RuntimeException("Пользователь с логином " + login + " не найден"));

        if (!login.equals(request.getLogin()) && userRepository.findByLogin(request.getLogin()).isPresent()) {
            throw new RuntimeException("Логин " + request.getLogin() + " уже занят");
        }

        boolean loginChanged = false;
        if (request.getLogin() != null && !request.getLogin().isEmpty() && !login.equals(request.getLogin())) {
            user.setLogin(request.getLogin());
            loginChanged = true;
        }
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            user.setEmail(request.getEmail());
        }
        if (request.getLastName() != null && !request.getLastName().isEmpty()) {
            user.setLastName(request.getLastName());
        }
        if (request.getFirstName() != null && !request.getFirstName().isEmpty()) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getMiddleName() != null) { // Отчество может быть пустым
            user.setMiddleName(request.getMiddleName());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        userRepository.save(user);

        // Если логин изменился, генерируем новый токен
        if (loginChanged) {
            return generateJwtToken(user);
        }
        return null; // Возвращаем null, если токен не нужно обновлять
    }

    public void changePassword(String login, String currentPassword, String newPassword) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new RuntimeException("Пользователь с логином " + login + " не найден"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Неверный текущий пароль");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private UserResponse convertToUserResponse(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        return new UserResponse(
                user.getLogin(),
                user.getEmail(),
                user.getLastName(),
                user.getFirstName(),
                user.getMiddleName(),
                user.getDateOfBirth(),
                roleNames
        );
    }

    public UserResponse getUserByLogin(String login) {
        System.out.println("Поиск пользователя с логином: " + login);
        try {
            User user = userRepository.findByLogin(login)
                    .orElseThrow(() -> new RuntimeException("Пользователь с логином " + login + " не найден"));
            System.out.println("Пользователь найден: " + user.getLogin());
            return convertToUserResponse(user);
        } catch (Exception e) {
            System.out.println("Ошибка в getUserByLogin: " + e.getMessage());
            throw e;
        }
    }

    private String generateJwtToken(User user) {
        return Jwts.builder()
                .setSubject(user.getLogin())
                .claim("roles", user.getRoles().stream().map(Role::getName).toList())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret)), SignatureAlgorithm.HS512)
                .compact();
    }
}