package com.diplom.agafonov.service;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Service
public class FiasDownloadService {
    @Value("D:\\Учёба\\Диплом\\Данные\\download")
    private String downloadDirectory;

    @Value("D:\\Учёба\\Диплом\\Данные\\extract")
    private String extractDirectory;

    private static final String BASE_URL = "https://fias-file.nalog.ru/downloads";
    private static final String FILE_NAME = "base.arj";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final int MAX_DAYS_TO_CHECK = 30;
    private static final String WINRAR_PATH = "C:\\Program Files\\WinRAR\\WinRAR.exe";
    private final OptimizedDbfImportService importService;

    @Autowired
    public FiasDownloadService(OptimizedDbfImportService importService) {
        this.importService = importService;
    }

    @Scheduled(cron = "0 0 3 * * MON")
    public void downloadAndExtractFiasData() throws IOException {
        String downloadUrl = findLatestFileUrl();
        if (downloadUrl == null) {
            throw new IOException("Не удалось найти актуальный файл ФИАС за последние " + MAX_DAYS_TO_CHECK + " дней");
        }

        System.out.println("Найден актуальный файл: " + downloadUrl);

        Path downloadPath = Paths.get(downloadDirectory, "fias_data.arj");

        try {
            downloadFile(downloadUrl, downloadPath.toString());
            extractWithWinRAR(downloadPath.toString(), extractDirectory);
            processFiasData();
        } catch (IOException e) {
            Files.deleteIfExists(downloadPath);
            throw e;
        }
    }

    private String findLatestFileUrl() {
        LocalDate currentDate = LocalDate.now();

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            for (int i = 0; i < MAX_DAYS_TO_CHECK; i++) {
                LocalDate checkDate = currentDate.minusDays(i);
                String dateStr = checkDate.format(DATE_FORMATTER);
                String fileUrl = String.format("%s/%s/%s", BASE_URL, dateStr, FILE_NAME);

                if (isFileAvailable(httpClient, fileUrl)) {
                    return fileUrl;
                }

                TimeUnit.MILLISECONDS.sleep(100);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Ошибка при поиске файла: " + e.getMessage());
        }

        return null;
    }

    private boolean isFileAvailable(CloseableHttpClient httpClient, String fileUrl) {
        try {
            HttpHead httpHead = new HttpHead(fileUrl);
            try (CloseableHttpResponse response = httpClient.execute(httpHead)) {
                return response.getStatusLine().getStatusCode() == 200;
            }
        } catch (IOException e) {
            return false;
        }
    }

    private void downloadFile(String url, String filePath) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);

        try (CloseableHttpResponse response = httpClient.execute(httpGet);
             InputStream inputStream = response.getEntity().getContent();
             FileOutputStream outputStream = new FileOutputStream(filePath)) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    private void extractWithWinRAR(String arjFilePath, String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        ProcessBuilder processBuilder = new ProcessBuilder(
                WINRAR_PATH, "x", "-ibck", "-y", arjFilePath, destDirectory
        );

        try {
            System.out.println("Распаковка...");
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IOException("WinRAR завершился с ошибкой. Код: " + exitCode);
            }else{
                System.out.println("Архив распакован");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Распаковка прервана", e);
        }

    }

    private void processFiasData() {
        try {
            // Очистка и перезапись всех данных
            //dbfImportService.importAllFromDirectory(extractDirectory);
            importService.importAllFromDirectory(extractDirectory);
            System.out.println("Все данные ФИАС успешно обновлены");
        } catch (Exception e) {
            System.err.println("Ошибка при обработке данных ФИАС: " + e.getMessage());
            throw new RuntimeException("Ошибка при обработке данных ФИАС", e);
        }
    }
}