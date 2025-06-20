package com.diplom.agafonov.repository;

import com.diplom.agafonov.entity.Kladr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface KladrRepository extends JpaRepository<Kladr, Long> {
}
