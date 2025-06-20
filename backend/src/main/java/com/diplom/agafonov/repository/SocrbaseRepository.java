package com.diplom.agafonov.repository;

import com.diplom.agafonov.entity.ES.SocrbaseES;
import com.diplom.agafonov.entity.Socrbase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SocrbaseRepository extends JpaRepository<Socrbase, Long> {

    List<Socrbase> findByLevel(int level);

    List<Socrbase> findByScNameIgnoreCase(String scName);

    Optional<SocrbaseES> findByScName(String scName);
}
