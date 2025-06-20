package com.diplom.agafonov.service;

import com.diplom.agafonov.entity.*;
import com.diplom.agafonov.entity.ES.*;
import com.diplom.agafonov.repository.*;
import com.diplom.agafonov.repository.ES.*;
import com.linuxense.javadbf.*;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.*;

@Service
public class OptimizedDbfImportService {
    private static final int POSTGRES_BATCH_SIZE = 3000;
    private static final int ES_BATCH_SIZE = 1500;
    private static final int LOGGING_FREQUENCY = 20000;
    private static final Charset DBF_CHARSET = Charset.forName("CP866");
    private static final int THREAD_POOL_SIZE =
            Math.min(Runtime.getRuntime().availableProcessors(), 4);
    private static final Logger logger = LoggerFactory.getLogger(OptimizedDbfImportService.class);

    // Репозитории
    private final JpaRepository<?, ?>[] allRepositories;
    private final ElasticsearchRepository<?, ?>[] allEsRepositories;
    private final EntityManager entityManager;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ExecutorService executorService;
    private final KladrRepository kladrRepository;
    private final AltnameRepository altnameRepository;
    private final StreetRepository streetRepository;
    private final DomaRepository domaRepository;
    private final NamemapRepository namemapRepository;
    private final SocrbaseRepository socrbaseRepository;
    private final KladrESRepository kladrESRepository;
    private final AltnameESRepository altnameESRepository;
    private final StreetESRepository streetESRepository;
    private final DomaESRepository domaESRepository;
    private final NamemapESRepository namemapESRepository;
    private final SocrbaseESRepository socrbaseESRepository;
    private final PlatformTransactionManager transactionManager;

    @Autowired
    public OptimizedDbfImportService(
            ElasticsearchOperations elasticsearchOperations,
            EntityManager entityManager, KladrRepository kladrRepository, AltnameRepository altnameRepository, StreetRepository streetRepository, DomaRepository domaRepository, NamemapRepository namemapRepository, SocrbaseRepository socrbaseRepository, KladrESRepository kladrESRepository, AltnameESRepository altnameESRepository, StreetESRepository streetESRepository, DomaESRepository domaESRepository, NamemapESRepository namemapESRepository, SocrbaseESRepository socrbaseESRepository, PlatformTransactionManager transactionManager) {
        this.kladrRepository = kladrRepository;
        this.altnameRepository = altnameRepository;
        this.streetRepository = streetRepository;
        this.domaRepository = domaRepository;
        this.namemapRepository = namemapRepository;
        this.socrbaseRepository = socrbaseRepository;
        this.kladrESRepository = kladrESRepository;
        this.altnameESRepository = altnameESRepository;
        this.streetESRepository = streetESRepository;
        this.domaESRepository = domaESRepository;
        this.namemapESRepository = namemapESRepository;
        this.socrbaseESRepository = socrbaseESRepository;
        this.transactionManager = transactionManager;

        this.allRepositories = new JpaRepository<?, ?>[]{kladrRepository, streetRepository, altnameRepository,
                domaRepository, namemapRepository, socrbaseRepository};
        this.allEsRepositories = new ElasticsearchRepository<?, ?>[]{kladrESRepository, altnameESRepository, streetESRepository,
                domaESRepository, namemapESRepository, socrbaseESRepository};
        this.elasticsearchOperations = elasticsearchOperations;
        this.entityManager = entityManager;
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    @Async
    @Transactional
    public CompletableFuture<Void> importAllFromDirectory(String directoryPath) throws IOException {
        long startTime = System.currentTimeMillis();

        try {
            File[] dbfFiles = getDbfFiles(directoryPath);
            if (dbfFiles == null || dbfFiles.length == 0) {
                logger.warn("No DBF files found in directory: {}", directoryPath);
                return CompletableFuture.completedFuture(null);
            }

            clearAllData();
            optimizeElasticsearchSettings();

            // Создаем транзакционный шаблон для выполнения в потоках
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

            List<CompletableFuture<Void>> futures = Arrays.stream(dbfFiles)
                    .map(file -> CompletableFuture.runAsync(() ->
                            transactionTemplate.execute(status -> {
                                try {
                                    processDbfFile(file);
                                    return null;
                                } catch (Exception e) {
                                    logger.error("Error processing file: {}", file.getName(), e);
                                    throw new RuntimeException("Error processing file: " + file.getName(), e);
                                }
                            }), executorService))
                    .collect(Collectors.toList());

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenRun(this::finalizeImport)
                    .thenRun(() -> {
                        long duration = System.currentTimeMillis() - startTime;
                        logger.info("Import completed in {} ms", duration);
                    });
        } finally {
            executorService.shutdown();
        }
    }

    private void processDbfFile(File file) throws IOException, DBFException {
        String filename = file.getName().toLowerCase();

        if (filename.contains("kladr")) {
            processEntities(file, this::createKladr, this::convertToKladrEs,
                    kladrRepository, kladrESRepository);
        } else if (filename.contains("street")) {
            processEntities(file, this::createStreet, this::convertToStreetEs,
                    streetRepository, streetESRepository);
        } else if (filename.contains("doma")) {
            processEntities(file, this::createDoma, this::convertToDomaEs,
                    domaRepository, domaESRepository);
        } else if (filename.contains("socrbase")) {
            processEntities(file, this::createSocrbase, this::convertToSocrbaseEs,
                    socrbaseRepository, socrbaseESRepository);
        } else if (filename.contains("namemap")) {
            processEntities(file, this::createNamemap, this::convertToNamemapEs,
                    namemapRepository, namemapESRepository);
        } else if (filename.contains("altname")) {
            processEntities(file, this::createAltname, this::convertToAltnameEs,
                    altnameRepository, altnameESRepository);
        }
    }

    private <T, E> void processEntities(File file,
                                        Function<Object[], T> entityCreator,
                                        Function<T, E> esConverter,
                                        JpaRepository<T, ?> jpaRepo,
                                        ElasticsearchRepository<E, ?> esRepo) throws IOException, DBFException {

        List<T> pgBatch = new ArrayList<>(POSTGRES_BATCH_SIZE);
        List<E> esBatch = new ArrayList<>(ES_BATCH_SIZE);
        long counter = 0;

        try (DBFReader reader = new DBFReader(new FileInputStream(file), DBF_CHARSET)) {
            Object[] row;
            while ((row = reader.nextRecord()) != null) {
                counter++;
                if (counter % LOGGING_FREQUENCY == 0) {
                    System.out.printf("Processed %,d records from %s%n", counter, file.getName());
                }

                T entity = entityCreator.apply(row);
                pgBatch.add(entity);
                esBatch.add(esConverter.apply(entity));

                if (pgBatch.size() >= POSTGRES_BATCH_SIZE) {
                    savePostgresBatch(pgBatch, jpaRepo);
                    pgBatch.clear();
                }

                if (esBatch.size() >= ES_BATCH_SIZE) {
                    saveEsBatch(esBatch, esRepo);
                    esBatch.clear();
                }
            }

            // Save remaining records
            if (!pgBatch.isEmpty()) savePostgresBatch(pgBatch, jpaRepo);
            if (!esBatch.isEmpty()) saveEsBatch(esBatch, esRepo);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected <T> void savePostgresBatch(List<T> batch, JpaRepository<T, ?> repository) {
        try {
            if (repository instanceof AltnameRepository) {
                List<Altname> toSave = new ArrayList<>();
                AltnameRepository altnameRepo = (AltnameRepository) repository;

                for (Altname altname : (List<Altname>) batch) {
                    if (!altnameRepo.existsByOldCode(altname.getOldCode())) {
                        toSave.add(altname);
                    } else {
                        logger.debug("Пропуск дубликата Altname с oldcode: {}",
                                altname.getOldCode());
                    }
                }

                if (!toSave.isEmpty()) {
                    altnameRepo.saveAll(toSave);
                }
            } else {
                repository.saveAll(batch);
            }

            repository.saveAll(batch);
            entityManager.flush();
            entityManager.clear();
        } catch (Exception e) {
            logger.error("Error saving PostgreSQL batch", e);
            throw new RuntimeException("Failed to save PostgreSQL batch", e);
        }
    }

    private <E> void saveEsBatch(List<E> batch, ElasticsearchRepository<E, ?> repository) {
        try {
            // Получаем класс сущности
            Class<E> entityClass = (Class<E>) getEntityClass(repository);
            logger.info("Сохранение партии из {} элементов в ES для {}", batch.size(), entityClass.getSimpleName());

            // Получаем имя индекса из аннотации @Document у класса сущности
            Document documentAnnotation = entityClass.getAnnotation(Document.class);
            if (documentAnnotation == null) {
                throw new IllegalStateException("Entity class " + entityClass.getName() +
                        " must be annotated with @Document");
            }

            String indexName = documentAnnotation.indexName();
            IndexCoordinates indexCoordinates = IndexCoordinates.of(indexName);

            // Подготавливаем пакетный запрос
            List<IndexQuery> queries = batch.stream()
                    .map(entity -> new IndexQueryBuilder()
                            .withId(getId(entity))
                            .withObject(entity)
                            .build())
                    .collect(Collectors.toList());

            // Выполняем пакетную вставку
            elasticsearchOperations.bulkIndex(queries, indexCoordinates);
        } catch (Exception e) {
            logger.error("Детальная ошибка при сохранении партии в ES:", e);
            throw new RuntimeException("Не удалось сохранить партию в ES", e);
        }
    }

    private String getId(Object entity) {
        try {
            // Пытаемся получить ID через рефлексию
            return entity.getClass().getMethod("getId").invoke(entity).toString();
        } catch (Exception e) {
            // Если метод getId() не существует, генерируем UUID
            return UUID.randomUUID().toString();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void clearAllData() {
        try {
            logger.info("Starting data cleanup...");

            // 1. Очистка PostgreSQL данных
            Arrays.stream(allRepositories).forEach(repo -> {
                logger.info("Clearing data in {}", repo.getClass().getSimpleName());
                repo.deleteAllInBatch();
            });

            // 2. Очистка Elasticsearch данных с обработкой ошибок
            Arrays.stream(allEsRepositories).forEach(repo -> {
                try {
                    logger.info("Clearing data in ES {}", repo.getClass().getSimpleName());
                    // Удаляем данные порциями для больших индексов
                    deleteInBatches(repo);
                } catch (Exception e) {
                    logger.error("Error clearing ES data in {}", repo.getClass().getSimpleName(), e);
                    throw new RuntimeException("Failed to clear ES data", e);
                }
            });

            logger.info("Data cleanup completed successfully");
        } catch (Exception e) {
            logger.error("Error during data cleanup", e);
            throw e;
        }
    }

    private <E, ID> void deleteInBatches(ElasticsearchRepository<E, ID> repository) {
        int batchSize = 10000;
        boolean hasMore = true;

        while (hasMore) {
            Page<E> page = repository.findAll(PageRequest.of(0, batchSize));
            if (page.isEmpty()) {
                hasMore = false;
            } else {
                repository.deleteAll(page.getContent());
                try {
                    Thread.sleep(500); // Небольшая пауза между пакетами
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during batch delete", e);
                }
            }
        }
    }

    private void optimizeElasticsearchSettings() {
        Arrays.stream(allEsRepositories).forEach(repo -> {
            Class<?> entityClass = getEntityClass(repo);
            IndexOperations indexOps = elasticsearchOperations.indexOps(entityClass);

            logger.info("Обработка индекса для {}", entityClass.getSimpleName());

            if (indexOps.exists()) {
                logger.info("Удаление существующего индекса для {}", entityClass.getSimpleName());
                indexOps.delete();
            }

            // Создаем настройки через Map
            Map<String, Object> settings = new HashMap<>();
            settings.put("index.refresh_interval", "-1");
            settings.put("number_of_replicas", "0");

            // Создаем индекс с настройками
            indexOps.create(settings);

            // Создаем и применяем маппинг
            org.springframework.data.elasticsearch.core.document.Document mapping = indexOps.createMapping();
            if (mapping != null) {
                indexOps.putMapping(mapping);
            }
        });
    }

    private void finalizeImport() {
        Arrays.stream(allEsRepositories).forEach(repo -> {
            Class<?> entityClass = getEntityClass(repo);
            IndexOperations indexOps = elasticsearchOperations.indexOps(entityClass);

            // Получаем текущие настройки
            Map<String, Object> currentSettings = indexOps.getSettings();

            // Создаем новые настройки
            Map<String, Object> newSettings = new HashMap<>();
            newSettings.put("index.refresh_interval", "30s");
            newSettings.put("number_of_replicas", "1");

            // Объединяем настройки
            Map<String, Object> mergedSettings = new HashMap<>(currentSettings);
            mergedSettings.putAll(newSettings);

            // Удаляем и пересоздаем индекс с новыми настройками
            indexOps.delete();
            indexOps.create(mergedSettings);

            indexOps.refresh();
            long count = repo.count();
            logger.info("Итоговое количество для {}: {}", repo.getClass().getSimpleName(), count);
        });
    }

    private Class<?> getEntityClass(ElasticsearchRepository<?, ?> repository) {
        // Создаем мапу для хранения соответствия репозиториев и классов сущностей
        Map<Class<?>, Class<?>> repoToEntityMap = new HashMap<>();
        repoToEntityMap.put(kladrESRepository.getClass(), KladrES.class);
        repoToEntityMap.put(altnameESRepository.getClass(), AltnameES.class);
        repoToEntityMap.put(streetESRepository.getClass(), StreetES.class);
        repoToEntityMap.put(domaESRepository.getClass(), DomaES.class);
        repoToEntityMap.put(namemapESRepository.getClass(), NamemapES.class);
        repoToEntityMap.put(socrbaseESRepository.getClass(), SocrbaseES.class);

        Class<?> entityClass = repoToEntityMap.get(repository.getClass());
        if (entityClass != null) {
            return entityClass;
        }

        throw new RuntimeException("Failed to determine entity class for repository: " + repository.getClass());
    }


    // Методы создания сущностей
    private Kladr createKladr(Object[] row) {
        Kladr k = new Kladr();
        k.setName(getString(row[0]));
        k.setSocr(getString(row[1]));
        k.setCode(getString(row[2]));
        k.setPostalIndex(getString(row[3]));
        k.setGninmb(getString(row[4]));
        k.setUno(getString(row[5]));
        k.setOcatd(getString(row[6]));
        k.setStatus(getInt(row[7]));
        return k;
    }

    private KladrES convertToKladrEs(Kladr entity) {
        KladrES es = new KladrES();
        es.setId(UUID.randomUUID().toString());
        es.setName(entity.getName());            // Маппинг поля name
        es.setSocr(entity.getSocr());
        es.setCode(entity.getCode());
        es.setPostalIndex(entity.getPostalIndex());
        es.setGninmb(entity.getGninmb());
        es.setUno(entity.getUno());
        es.setOcatd(entity.getOcatd());
        es.setStatus(entity.getStatus());
        return es;
    }

    private Altname createAltname(Object[] row) {
        Altname k = new Altname();
        k.setOldCode(getString(row[0]));
        k.setNewCode(getString(row[1]));
        k.setLevel(getInt(row[2]));
        return k;
    }

    private AltnameES convertToAltnameEs(Altname entity) {
        AltnameES es = new AltnameES();
        es.setId(UUID.randomUUID().toString());
        es.setOldCode(entity.getOldCode());
        es.setNewCode(entity.getNewCode());
        es.setLevel(entity.getLevel());
        return es;
    }

    private Street createStreet(Object[] row) {
        Street k = new Street();
        k.setName(getString(row[0]));
        k.setSocr(getString(row[1]));
        k.setCode(getString(row[2]));
        k.setPostalIndex(getString(row[3]));
        k.setGninmb(getString(row[4]));
        k.setUno(getString(row[5]));
        k.setOcatd(getString(row[6]));
        return k;
    }

    private StreetES convertToStreetEs(Street entity) {
        StreetES es = new StreetES();
        es.setId(UUID.randomUUID().toString());
        es.setName(entity.getName());
        es.setSocr(entity.getSocr());
        es.setCode(entity.getCode());
        es.setPostalIndex(entity.getPostalIndex());
        es.setGninmb(entity.getGninmb());
        es.setUno(entity.getUno());
        es.setOcatd(entity.getOcatd());
        return es;
    }

    private Doma createDoma(Object[] row) {
        Doma k = new Doma();
        k.setName(getString(row[0]));
        k.setKorp(getString(row[1]));
        k.setSocr(getString(row[2]));
        k.setCode(getString(row[3]));
        k.setPostalIndex(getString(row[4]));
        k.setGninmb(getString(row[5]));
        k.setUno(getString(row[6]));
        k.setOcatd(getString(row[7]));
        return k;
    }

    private DomaES convertToDomaEs(Doma entity) {
        DomaES es = new DomaES();
        es.setId(UUID.randomUUID().toString());
        es.setName(entity.getName());
        es.setKorp(entity.getKorp());
        es.setSocr(entity.getSocr());
        es.setCode(entity.getCode());
        es.setPostalIndex(entity.getPostalIndex());
        es.setGninmb(entity.getGninmb());
        es.setUno(entity.getUno());
        es.setOcatd(entity.getOcatd());
        return es;
    }

    private Namemap createNamemap(Object[] row) {
        Namemap k = new Namemap();
        k.setCode(getString(row[0]));
        k.setName(getString(row[1]));
        k.setShortName(getString(row[2]));
        k.setScName(getString(row[3]));
        return k;
    }

    private NamemapES convertToNamemapEs(Namemap entity) {
        NamemapES es = new NamemapES();
        es.setId(UUID.randomUUID().toString());
        es.setCode(entity.getCode());
        es.setName(entity.getName());
        es.setShortName(entity.getShortName());
        es.setScName(entity.getScName());
        return es;
    }

    private Socrbase createSocrbase(Object[] row) {
        Socrbase k = new Socrbase();
        k.setLevel(getInt(row[0]));
        k.setScName(getString(row[1]));
        k.setSocrName(getString(row[2]));
        k.setKodTSt(getString(row[3]));
        return k;
    }

    private SocrbaseES convertToSocrbaseEs(Socrbase entity) {
        SocrbaseES es = new SocrbaseES();
        es.setId(UUID.randomUUID().toString());
        es.setLevel(entity.getLevel());
        es.setScName(entity.getScName());
        es.setSocrName(entity.getSocrName());
        es.setKodTSt(entity.getKodTSt());
        return es;
    }

    private String getString(Object value) {
        return value != null ? value.toString().trim() : null;
    }

    private Integer getInt(Object value) {
        try {
            return value instanceof Number ? ((Number)value).intValue()
                    : value != null ? Integer.parseInt(value.toString().trim()) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private File[] getDbfFiles(String dirPath) {
        File dir = new File(dirPath);
        return dir.listFiles((d, name) -> name.toLowerCase().endsWith(".dbf"));
    }
}
