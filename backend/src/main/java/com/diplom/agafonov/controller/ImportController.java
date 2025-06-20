package com.diplom.agafonov.controller;
import com.diplom.agafonov.service.FiasDownloadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/import")
@Tag(name = "Импорт данных ФИАС", description = "API для работы с данными ФИАС")
@CrossOrigin(origins = {"http://localhost:9000", "http://localhost:8080", "http://localhost:3000"})
public class ImportController {

    private final FiasDownloadService fiasDownloadService;

    @Autowired
    public ImportController(FiasDownloadService fiasDownloadService) {
        this.fiasDownloadService = fiasDownloadService;
    }

    @Operation(summary = "Обновление данных из ФИАС",
            description = "Скачивает последнюю версию данных с сервера ФИАС и обновляет БД и Elasticsearch")
    @PostMapping("/update-from-fias")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateFromFias() {
        try {
            fiasDownloadService.downloadAndExtractFiasData();
            return ResponseEntity.ok("Данные успешно загружены с сервера ФИАС и обновлены в PostgreSQL и Elasticsearch");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Ошибка обновления данных из ФИАС: " + e.getMessage());
        }
    }

    @PostMapping("/update-from-fias-test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> startImport() {
        try {
            fiasDownloadService.downloadAndExtractFiasData();
            return ResponseEntity.ok("Import started successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Import failed: " + e.getMessage());
        }
    }
}
