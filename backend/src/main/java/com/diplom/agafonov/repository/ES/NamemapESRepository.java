package com.diplom.agafonov.repository.ES;

import com.diplom.agafonov.entity.ES.NamemapES;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NamemapESRepository extends ElasticsearchRepository<NamemapES, String> {
}
