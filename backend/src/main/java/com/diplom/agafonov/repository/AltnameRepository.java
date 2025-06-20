package com.diplom.agafonov.repository;

import com.diplom.agafonov.entity.Altname;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface AltnameRepository extends JpaRepository<Altname, Long> {
    boolean existsByOldCode(String oldCode);
}
