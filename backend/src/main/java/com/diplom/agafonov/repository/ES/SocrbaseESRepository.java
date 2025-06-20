package com.diplom.agafonov.repository.ES;

import com.diplom.agafonov.entity.ES.SocrbaseES;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SocrbaseESRepository extends ElasticsearchRepository<SocrbaseES, String> {
    @Query("{\"term\": {\"level\": ?0}}")
    List<SocrbaseES> findByLevel(Integer level);

    Optional<SocrbaseES> findByScName(String scName);
}
