package com.diplom.agafonov.repository;

import com.diplom.agafonov.entity.Kladr;
import com.diplom.agafonov.entity.Namemap;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NamemapRepository extends JpaRepository<Namemap, Long> {

    List<Namemap> findTop10ByShortNameContainingIgnoreCase(String name, Pageable pageable);

    List<Namemap> findByShortNameContainingIgnoreCase(String name);
}
