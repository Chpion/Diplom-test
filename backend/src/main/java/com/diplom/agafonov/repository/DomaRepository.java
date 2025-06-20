package com.diplom.agafonov.repository;

import com.diplom.agafonov.entity.Doma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DomaRepository extends JpaRepository<Doma, Long> {
}
