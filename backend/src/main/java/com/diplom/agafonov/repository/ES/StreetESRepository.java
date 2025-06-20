package com.diplom.agafonov.repository.ES;

import com.diplom.agafonov.entity.ES.StreetES;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StreetESRepository extends ElasticsearchRepository<StreetES, String> {
    @Query("{\"match\": {\"name\": {\"query\": \"?0\", \"analyzer\": \"russian_analyzer\"}}}, \"size\": 10}")
    List<StreetES> findByNameContaining(String name);

    @Query("{\"bool\": {\"filter\": [{\"prefix\": {\"code\": \"?0\"}}, {\"match\": {\"name\": {\"query\": \"?1\", \"analyzer\": \"russian_analyzer\"}}}]}}")
    List<StreetES> findByCodeStartingWithAndNameContaining(String codePrefix, String name);

    @Query("{\"bool\": {\"filter\": [{\"match\": {\"name\": \"?0\"}}, {\"match\": {\"socr\": \"?1\"}}, {\"prefix\": {\"code\": \"?2\"}}]}}")
    Optional<StreetES> findByNameAndSocrAndCodeStartingWith(String name, String socr, String codePrefix);

    @Query("{\"bool\": {\"filter\": [{\"match\": {\"name\": \"?0\"}}, {\"match\": {\"socr\": \"?1\"}}]}}")
    Optional<StreetES> findByNameAndSocr(String name, String socr);

    @Query("{\"bool\": {\"filter\": [{\"prefix\": {\"code\": \"?0\"}}]}}")
    List<StreetES> findByCodeStartingWith(String codePrefix);
}