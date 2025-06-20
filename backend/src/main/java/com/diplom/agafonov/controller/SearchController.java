package com.diplom.agafonov.controller;

import com.diplom.agafonov.service.AuthService;
import com.diplom.agafonov.service.SearchServiceES;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/search")
@Tag(name = "Поиск данных", description = "API для поиска адресных объектов")
@CrossOrigin(origins = {"http://localhost:9000", "http://localhost:8080", "http://localhost:3000"})
public class SearchController {

    private final SearchServiceES searchServiceES;
    private final AuthService authService;
    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

    @Autowired
    public SearchController(SearchServiceES searchServiceES, AuthService authService) {
        this.searchServiceES = searchServiceES;
        this.authService = authService;
    }

    @Operation(summary = "Поиск по ключевому слову с Elasticsearch",
            description = "Полнотекстовый поиск по названиям объектов с учетом приоритетов типов")
    @GetMapping("/bySearchES")
    public CompletableFuture<ResponseEntity<List<String>>> searchES(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "100") int limit) {
        return searchServiceES.search(keyword, limit)
                .thenApply(ResponseEntity::ok)
                .exceptionally(e -> ResponseEntity.internalServerError().build());
    }

    @Operation(summary = "Расширенный поиск по частям адреса",
            description = "Поиск адресов по отдельным компонентам (регион, город, улица, дом)")
    @GetMapping("/advancedSearch")
    public CompletableFuture<ResponseEntity<List<String>>> advancedSearch(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String street,
            @RequestParam(required = false) String house,
            @RequestParam(defaultValue = "100") int limit) {
        return searchServiceES.advancedSearch(region, city, street, house, limit)
                .thenApply(ResponseEntity::ok)
                .exceptionally(e -> ResponseEntity.internalServerError().build());
    }

    @Operation(summary = "Получение информации об адресе",
            description = "Возвращает полную информацию об адресе по ключевому слову.")
    @GetMapping("/getAddressInfo")
    public ResponseEntity<Map<String, Object>> getAddressInfo(@RequestParam String keyword) {
        try {
            String decodedKeyword = java.net.URLDecoder.decode(keyword, StandardCharsets.UTF_8.name());
            logger.debug("Received keyword: {}, Decoded keyword: {}", keyword, decodedKeyword);
            Map<String, Object> addressInfo = searchServiceES.getAddressInfo(decodedKeyword);
            logger.debug("Address info retrieved: {}", addressInfo);
            if (addressInfo.containsKey("error")) {
                return ResponseEntity.badRequest().body(addressInfo);
            }
            return ResponseEntity.ok(addressInfo);
        } catch (Exception e) {
            logger.error("Error retrieving address info for keyword {}: {}", keyword, e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Ошибка получения информации об адресе: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
