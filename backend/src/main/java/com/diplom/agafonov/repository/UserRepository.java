package com.diplom.agafonov.repository;

import com.diplom.agafonov.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLogin(String login);

    @Query("SELECT u FROM User u WHERE LOWER(u.login) LIKE :pattern OR LOWER(u.email) LIKE :pattern OR LOWER(u.firstName) LIKE :pattern OR LOWER(u.lastName) LIKE :pattern")
    List<User> findByLoginLikeOrEmailLikeOrFirstNameLikeOrLastNameLike(@Param("pattern") String pattern);
}
