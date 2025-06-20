package com.diplom.agafonov.service;

import com.diplom.agafonov.entity.ES.DomaES;
import com.diplom.agafonov.entity.ES.KladrES;
import com.diplom.agafonov.entity.ES.SocrbaseES;
import com.diplom.agafonov.entity.ES.StreetES;
import com.diplom.agafonov.repository.ES.DomaESRepository;
import com.diplom.agafonov.repository.ES.KladrESRepository;
import com.diplom.agafonov.repository.ES.SocrbaseESRepository;
import com.diplom.agafonov.repository.ES.StreetESRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SearchServiceESTest {

    @Mock
    private KladrESRepository kladrESRepository;

    @Mock
    private StreetESRepository streetESRepository;

    @Mock
    private SocrbaseESRepository socrbaseESRepository;

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private DomaESRepository domaESRepository;

    @InjectMocks
    private SearchServiceES searchServiceES;

    @BeforeEach
    void setUp() {
        try {
            java.lang.reflect.Field cachedSocrPriorityMapField = SearchServiceES.class.getDeclaredField("cachedSocrPriorityMap");
            cachedSocrPriorityMapField.setAccessible(true);
            cachedSocrPriorityMapField.set(searchServiceES, null);
            java.lang.reflect.Field cachedSettlementSocrsField = SearchServiceES.class.getDeclaredField("cachedSettlementSocrs");
            cachedSettlementSocrsField.setAccessible(true);
            cachedSettlementSocrsField.set(searchServiceES, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up test", e);
        }
    }
    @Test
    void getAddressInfo_regionFound() {
        KladrES region = new KladrES();
        region.setName("Ульяновская");
        region.setSocr("обл");
        region.setCode("7300000000000");
        region.setStatus(1);
        when(kladrESRepository.findByNameAndSocr("ульяновская", "обл")).thenReturn(Optional.of(region));
        Map<String, Object> result = searchServiceES.getAddressInfo("обл. Ульяновская");
        assertEquals("Ульяновская", result.get("name"));
        assertEquals("обл", result.get("socr"));
        assertEquals("7300000000000", result.get("code"));
        assertEquals(1, result.get("status"));
        assertFalse(result.containsKey("error"));
    }
    @Test
    void getAddressInfo_regionNotFound() {
        when(kladrESRepository.findByNameAndSocr("ульяновская", "обл")).thenReturn(Optional.empty());
        Map<String, Object> result = searchServiceES.getAddressInfo("обл. Ульяновская");
        assertTrue(result.containsKey("error"));
        assertEquals("Регион не найден: обл. ульяновская", result.get("error"));
    }
    @Test
    void getAddressInfo_cityFound() {
        KladrES region = new KladrES();
        region.setName("Ульяновская");
        region.setSocr("обл");
        region.setCode("7300000000000");
        KladrES city = new KladrES();
        city.setName("Ульяновск");
        city.setSocr("г");
        city.setCode("7300000100000");
        city.setStatus(1);
        when(kladrESRepository.findByNameAndSocr("ульяновская", "обл")).thenReturn(Optional.of(region));
        when(kladrESRepository.findByNameAndSocrAndCodeStartingWith("ульяновск", "г", "73")).thenReturn(Optional.of(city));
        Map<String, Object> result = searchServiceES.getAddressInfo("обл. Ульяновская, г. Ульяновск");
        assertEquals("Ульяновск", result.get("name"));
        assertEquals("г", result.get("socr"));
        assertEquals("7300000100000", result.get("code"));
        assertEquals(1, result.get("status"));
        assertFalse(result.containsKey("error"));
    }

    @Test
    void getAddressInfo_emptyKeyword() {
        Map<String, Object> result = searchServiceES.getAddressInfo("");

        assertTrue(result.containsKey("error"));
        assertEquals("Адрес не найден", result.get("error"));
    }
    @Test
    void searchAddressesWithPath_success() {
        SocrbaseES socr1 = new SocrbaseES();
        socr1.setScName("обл");
        socr1.setLevel(1);
        SocrbaseES socr2 = new SocrbaseES();
        socr2.setScName("г");
        socr2.setLevel(4);
        when(socrbaseESRepository.findAll()).thenReturn(List.of(socr1, socr2));
        KladrES region = new KladrES();
        region.setName("Ульяновская");
        region.setSocr("обл");
        region.setCode("7300000000000");
        KladrES city = new KladrES();
        city.setName("Ульяновск");
        city.setSocr("г");
        city.setCode("7300000100000");
        when(kladrESRepository.findByNameContaining("ульянов")).thenReturn(List.of(region, city));
        StreetES street = new StreetES();
        street.setName("Ульяновская");
        street.setSocr("ул");
        street.setCode("73000001000001200");
        when(streetESRepository.findByNameContaining("ульянов")).thenReturn(List.of(street));
        List<String> results = searchServiceES.searchAddressesWithPath("ульянов", 10);
        assertEquals(3, results.size());
        assertTrue(results.contains("обл. Ульяновская"));
        assertTrue(results.contains("г. Ульяновск"));
        assertTrue(results.contains("ул. Ульяновская"));
    }
    @Test
    void search_withDom() throws Exception {
        CompletableFuture<List<String>> future = searchServiceES.search("г. Ульяновск, ул. Ленина, дом. 10", 10);
        List<String> results = future.get();
        assertEquals(1, results.size());
        assertEquals("Всё", results.get(0));
    }
    @Test
    void advancedSearch_emptyRegion() throws Exception {
        CompletableFuture<List<String>> future = searchServiceES.advancedSearch("", "Ульяновск", null, null, 10);
        List<String> results = future.get();
        assertTrue(results.isEmpty());
    }
}