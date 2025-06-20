package com.diplom.agafonov.repository;

import com.diplom.agafonov.entity.Kladr;
import com.diplom.agafonov.entity.Street;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface StreetRepository extends JpaRepository<Street, Long> {
    @Query("SELECT s.code FROM Street s")
    Set<String> findAllCodes();

    List<Street> findTop10ByNameContainingIgnoreCase(String name, Pageable pageable);

    List<Street> findByNameContainingIgnoreCase(String name);

    List<Street> findBySocrAndNameContainingIgnoreCase(String socr, String name);

    List<Street> findByCodeContainingIgnoreCase(String code);

    List<Street> findBySocrAndName(String socr, String name);

    List<Street> findByCodeStartingWith(String codePrefix);
}
