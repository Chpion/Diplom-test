package com.diplom.agafonov.repository.ES;

import com.diplom.agafonov.entity.ES.DomaES;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DomaESRepository extends ElasticsearchRepository<DomaES, String> {
    @Query("{\"bool\": {\"filter\": [{\"prefix\": {\"code\": \"?0\"}}]}}")
    List<DomaES> findByCodeStartingWith(String codePrefix);

    @Query("{\"bool\": {\"filter\": [{\"prefix\": {\"code\": \"?0\"}}, {\"match\": {\"name\": \"?1\"}}]}}")
    List<DomaES> findByCodeStartingWithAndNameContaining(String codePrefix, String name);

    @Query("{\"match\": {\"name\": {\"query\": \"?0\", \"analyzer\": \"standard\"}}}, \"size\": 10}")
    List<DomaES> findByNameContaining(String name);
}