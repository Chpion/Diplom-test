package com.diplom.agafonov.repository.ES;

import com.diplom.agafonov.entity.ES.KladrES;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface KladrESRepository extends ElasticsearchRepository<KladrES, String> {
    @Query("{\"match\": {\"name\": {\"query\": \"?0\", \"analyzer\": \"standard\"}}}, \"size\": 10}")
    List<KladrES> findByNameContaining(String name);

    @Query("{\"bool\": {\"filter\": [{\"prefix\": {\"code\": \"?0\"}}, {\"terms\": {\"socr\": ?1}}, {\"match\": {\"name\": \"?2\"}}]}}")
    List<KladrES> findByCodeStartingWithAndSocrInAndNameContaining(String codePrefix, Set<String> socrs, String keyword);

    Optional<KladrES> findByNameAndSocr(String name, String socr);

    @Query("{\"bool\": {\"filter\": [{\"match\": {\"name\": \"?0\"}}, {\"match\": {\"socr\": \"?1\"}}, {\"prefix\": {\"code\": \"?2\"}}]}}")
    Optional<KladrES> findByNameAndSocrAndCodeStartingWith(String name, String socr, String codePrefix);

    @Query("{\"term\": {\"code\": \"?0\"}}")
    Optional<KladrES> findFirstByCode(String code);

    @Query("{\"match\": {\"code\": \"?0\"}}, \"size\": 10}")
    List<KladrES> findByCodeStartingWith(String codePrefix);

    @Query("{\"bool\": {\"filter\": [{\"prefix\": {\"code\": \"?0\"}}, {\"terms\": {\"socr\": ?1}}]}}")
    List<KladrES> findByCodeStartingWithAndSocrIn(String codePrefix, Set<String> socrs);
}