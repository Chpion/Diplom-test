package com.diplom.agafonov.repository.ES;


import com.diplom.agafonov.entity.ES.AltnameES;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AltnameESRepository extends ElasticsearchRepository<AltnameES, String> {
}
