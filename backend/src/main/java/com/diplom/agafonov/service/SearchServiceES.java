package com.diplom.agafonov.service;

import com.diplom.agafonov.entity.ES.DomaES;
import com.diplom.agafonov.entity.ES.KladrES;
import com.diplom.agafonov.entity.ES.SocrbaseES;
import com.diplom.agafonov.entity.ES.StreetES;
import com.diplom.agafonov.repository.ES.DomaESRepository;
import com.diplom.agafonov.repository.ES.KladrESRepository;
import com.diplom.agafonov.repository.ES.SocrbaseESRepository;
import com.diplom.agafonov.repository.ES.StreetESRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
public class SearchServiceES {
    private static final Logger logger = LoggerFactory.getLogger(SearchServiceES.class);
    private final KladrESRepository kladrESRepository;
    private final StreetESRepository streetESRepository;
    private final SocrbaseESRepository socrbaseESRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final DomaESRepository domaESRepository;
    private Map<String, Integer> cachedSocrPriorityMap;
    private Set<String> cachedSettlementSocrs = null;

    @Autowired
    public SearchServiceES(KladrESRepository kladrESRepository,
                           StreetESRepository streetESRepository,
                           SocrbaseESRepository socrbaseESRepository,
                           ElasticsearchOperations elasticsearchOperations, DomaESRepository domaESRepository) {
        this.kladrESRepository = kladrESRepository;
        this.streetESRepository = streetESRepository;
        this.socrbaseESRepository = socrbaseESRepository;
        this.elasticsearchOperations = elasticsearchOperations;
        this.domaESRepository = domaESRepository;
    }

    public Map<String, Object> getAddressInfo(String keyword) {
        logger.debug("Processing getAddressInfo for keyword: {}", keyword);
        Map<String, Object> result = new HashMap<>();
        if (keyword == null || keyword.trim().isEmpty()) {
            logger.warn("Empty or null keyword provided");
            result.put("error", "Адрес не найден");
            return result;
        }
        try {
            String normalizedKeyword = keyword.trim().toLowerCase();
            logger.debug("Normalized keyword: {}", normalizedKeyword);

            // Разделяем адрес на части
            String[] parts = normalizedKeyword.split(",\\s*");
            if (parts.length == 0) {
                logger.warn("No valid address parts found in keyword: {}", normalizedKeyword);
                result.put("error", "Адрес не найден");
                return result;
            }

            String regionPart = null, cityPart = null, streetPart = null, housePart = null;
            for (String part : parts) {
                if (part.matches(".*обл\\..*")) {
                    regionPart = part;
                } else if (part.matches(".*г\\..*")) {
                    cityPart = part;
                } else if (part.matches(".*(ул\\.|пр-кт\\.|б-р\\.|пер\\.|ш\\.|пл\\.).*")) {
                    streetPart = part;
                } else if (part.matches(".*дом\\..*")) {
                    housePart = part;
                }
            }

            // Поиск региона (если указан)
            String regionCodePrefix = null;
            KladrES region = null;
            if (regionPart != null) {
                String[] regionSocrAndName = regionPart.split("\\.", 2);
                if (regionSocrAndName.length == 2) {
                    String socr = regionSocrAndName[0].trim();
                    String name = regionSocrAndName[1].trim().replaceAll(",$", "");
                    logger.debug("Searching region: socr={}, name={}", socr, name);
                    Optional<KladrES> regionOpt = kladrESRepository.findByNameAndSocr(name, socr);
                    if (regionOpt.isPresent()) {
                        region = regionOpt.get();
                        regionCodePrefix = region.getCode().substring(0, 2);
                        logger.debug("Found region: {}, code prefix: {}", region, regionCodePrefix);
                    } else {
                        logger.warn("Region not found: socr={}, name={}", socr, name);
                        result.put("error", "Регион не найден: " + regionPart);
                        return result;
                    }
                }
            }

            // Поиск города (если указан)
            String cityCodePrefix = null;
            KladrES city = null;
            if (cityPart != null) {
                String[] citySocrAndName = cityPart.split("\\.", 2);
                if (citySocrAndName.length == 2) {
                    String socr = citySocrAndName[0].trim();
                    String name = citySocrAndName[1].trim().replaceAll(",$", "");
                    logger.debug("Searching city: socr={}, name={}", socr, name);
                    Optional<KladrES> cityOpt = regionCodePrefix != null
                            ? kladrESRepository.findByNameAndSocrAndCodeStartingWith(name, socr, regionCodePrefix)
                            : kladrESRepository.findByNameAndSocr(name, socr);
                    if (cityOpt.isPresent()) {
                        city = cityOpt.get();
                        cityCodePrefix = city.getCode().substring(0, 8);
                        logger.debug("Found city: {}, code prefix: {}", city, cityCodePrefix);
                    } else {
                        logger.warn("City not found: socr={}, name={}", socr, name);
                        result.put("error", "Город не найден: " + cityPart);
                        return result;
                    }
                }
            }

            // Поиск улицы (если указана)
            String streetCode = null;
            StreetES street = null;
            if (streetPart != null) {
                String[] streetSocrAndName = streetPart.split("\\.", 2);
                if (streetSocrAndName.length == 2) {
                    String socr = streetSocrAndName[0].trim().replaceAll("\\.$", "").toLowerCase();
                    String name = streetSocrAndName[1].trim().replaceAll(",$", "");
                    logger.debug("Searching street: socr={}, name={}", socr, name);

                    List<StreetES> streets = cityCodePrefix != null
                            ? streetESRepository.findByCodeStartingWithAndNameContaining(cityCodePrefix, name)
                            : streetESRepository.findByNameContaining(name);
                    Optional<StreetES> streetOpt = streets.stream().filter(s -> s.getSocr().equals(socr) || s.getSocr().equals(socr.equals("пр-кт") ? "пр-т" : "пр-кт")).findFirst();

                    if (streetOpt.isPresent()) {
                        street = streetOpt.get();
                        streetCode = street.getCode().substring(0, 15);
                        logger.debug("Found street: {}, code: {}", street, streetCode);
                    } else {
                        String altSocr = socr.equals("пр-кт") ? "пр-т" : "пр-кт";
                        logger.debug("Trying alternative socr: {}", altSocr);
                        streets = cityCodePrefix != null
                                ? streetESRepository.findByCodeStartingWithAndNameContaining(cityCodePrefix, name)
                                : streetESRepository.findByNameContaining(name);
                        streetOpt = streets.stream().filter(s -> s.getSocr().equals(altSocr)).findFirst();

                        if (streetOpt.isPresent()) {
                            street = streetOpt.get();
                            streetCode = street.getCode().substring(0, 15);
                            logger.debug("Found street with alternative socr: {}, code: {}", street, streetCode);
                        } else {
                            logger.warn("Street not found: socr={}, name={}", socr, name);
                            result.put("error", "Улица не найдена: " + streetPart);
                            return result;
                        }
                    }
                }
            }

            // Поиск домов (если указаны)
            if (housePart != null) {
                String[] houseSocrAndNames = housePart.split("\\.", 2);
                if (houseSocrAndNames.length == 2) {
                    String socr = houseSocrAndNames[0].trim();
                    String[] houseNames = houseSocrAndNames[1].trim().replaceAll(",$", "").split(",");
                    logger.debug("Searching houses: socr={}, names={}", socr, String.join(", ", houseNames));
                    List<Map<String, Object>> houseResults = new ArrayList<>();
                    for (String houseName : houseNames) {
                        houseName = houseName.trim();
                        List<DomaES> singleHouseResults = streetCode != null
                                ? domaESRepository.findByCodeStartingWithAndNameContaining(streetCode, houseName)
                                : domaESRepository.findByNameContaining(houseName);
                        houseResults.addAll(singleHouseResults.stream().map(house -> {
                            Map<String, Object> houseData = new HashMap<>();
                            houseData.put("name", house.getName());
                            houseData.put("socr", house.getSocr());
                            houseData.put("korp", house.getKorp() != null ? house.getKorp() : "N/A");
                            houseData.put("code", house.getCode());
                            houseData.put("ocatd", house.getOcatd() != null ? house.getOcatd() : "N/A");
                            houseData.put("postalIndex", house.getPostalIndex() != null ? house.getPostalIndex() : "N/A");
                            houseData.put("gninmb", house.getGninmb() != null ? house.getGninmb() : "N/A");
                            houseData.put("uno", house.getUno() != null ? house.getUno() : "N/A");
                            return houseData;
                        }).collect(Collectors.toList()));
                        logger.debug("Found {} houses for name: {}", singleHouseResults.size(), houseName);
                    }
                    if (!houseResults.isEmpty()) {
                        if (houseResults.size() == 1) {
                            return houseResults.get(0);
                        } else {
                            Map<String, Object> combined = new HashMap<>();
                            combined.put("name", houseResults.stream()
                                    .map(h -> h.get("name").toString())
                                    .collect(Collectors.joining(", ")));
                            combined.put("socr", socr);
                            combined.put("korp", houseResults.stream()
                                    .map(h -> h.get("korp").toString())
                                    .collect(Collectors.joining(", ")));
                            combined.put("code", houseResults.stream()
                                    .map(h -> h.get("code").toString())
                                    .collect(Collectors.joining(", ")));
                            combined.put("ocatd", houseResults.stream()
                                    .map(h -> h.get("ocatd").toString())
                                    .collect(Collectors.joining(", ")));
                            combined.put("postalIndex", houseResults.stream()
                                    .map(h -> h.get("postalIndex").toString())
                                    .collect(Collectors.joining(", ")));
                            combined.put("gninmb", houseResults.stream()
                                    .map(h -> h.get("gninmb").toString())
                                    .collect(Collectors.joining(", ")));
                            combined.put("uno", houseResults.stream()
                                    .map(h -> h.get("uno").toString())
                                    .collect(Collectors.joining(", ")));
                            return combined;
                        }
                    } else {
                        logger.warn("Houses not found: socr={}, names={}", socr, String.join(", ", houseNames));
                        result.put("error", "Дома не найдены: " + housePart);
                        return result;
                    }
                }
            }

            // Если дома не указаны, возвращаем последний найденный элемент
            if (street != null) {
                result.put("name", street.getName());
                result.put("socr", street.getSocr());
                result.put("korp", "N/A");
                result.put("code", street.getCode());
                result.put("ocatd", street.getOcatd() != null ? street.getOcatd() : "N/A");
                result.put("postalIndex", street.getPostalIndex() != null ? street.getPostalIndex() : "N/A");
                result.put("gninmb", street.getGninmb() != null ? street.getGninmb() : "N/A");
                result.put("uno", street.getUno() != null ? street.getUno() : "N/A");
                return result;
            }
            if (city != null) {
                result.put("name", city.getName());
                result.put("socr", city.getSocr());
                result.put("korp", "N/A");
                result.put("code", city.getCode());
                result.put("ocatd", city.getOcatd() != null ? city.getOcatd() : "N/A");
                result.put("postalIndex", city.getPostalIndex() != null ? city.getPostalIndex() : "N/A");
                result.put("gninmb", city.getGninmb() != null ? city.getGninmb() : "N/A");
                result.put("uno", city.getUno() != null ? city.getUno() : "N/A");
                result.put("status", city.getStatus());
                return result;
            }
            if (region != null) {
                result.put("name", region.getName());
                result.put("socr", region.getSocr());
                result.put("korp", "N/A");
                result.put("code", region.getCode());
                result.put("ocatd", region.getOcatd() != null ? region.getOcatd() : "N/A");
                result.put("postalIndex", region.getPostalIndex() != null ? region.getPostalIndex() : "N/A");
                result.put("gninmb", region.getGninmb() != null ? region.getGninmb() : "N/A");
                result.put("uno", region.getUno() != null ? region.getUno() : "N/A");
                result.put("status", region.getStatus());
                return result;
            }

            logger.warn("No address info found for keyword: {}", normalizedKeyword);
            result.put("error", "Адрес не найден");
            return result;
        } catch (Exception e) {
            logger.error("Error retrieving address info for keyword {}: {}", keyword, e.getMessage(), e);
            result.put("error", "Ошибка поиска адреса: " + e.getMessage());
            return result;
        }
    }

    @Async
    public CompletableFuture<List<String>> advancedSearch(String region, String city, String street, String house, int limit) {
        StringBuilder searchQuery = new StringBuilder();
        if (region != null && !region.isEmpty()) {
            searchQuery.append(region);
            if (city != null && !city.isEmpty()) {
                searchQuery.append(", ").append(city);
                if (street != null && !street.isEmpty()) {
                    searchQuery.append(", ").append(street);
                    if (house != null && !house.isEmpty()) {
                        searchQuery.append(", ").append(house);
                    }
                }
            }
        } else {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        logger.debug("Advanced search query: {}", searchQuery);
        return search(searchQuery.toString(), limit);
    }

    @Async
    public CompletableFuture<List<String>> search(String request, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            logger.debug("Processing search request: {}", request);
            if (!request.trim().contains(" ")) {
                return searchAddressesWithPath(request, limit);
            } else {
                String[] parts = request.split(", ");
                if (parts.length == 0) {
                    return Collections.emptyList();
                }
                String lastPart = parts[parts.length - 1].trim();
                String[] socrAndName = lastPart.split("\\.", 2);
                if (socrAndName.length == 2) {
                    if (!socrAndName[0].trim().toLowerCase().equals("дом")) {
                        return getChildAddressElements(request, null);
                    } else {
                        return Arrays.asList("Всё");
                    }
                } else {
                    return getChildAddressElements(request, lastPart);
                }
            }
        });
    }

    public List<String> searchAddressesWithPath(String keyword, int limit) {
        try {
            Map<String, Integer> socrPriority = getSocrPriorityMap();
            logger.debug("SocrPriorityMap size: {}", socrPriority.size());

            // Нормализация строки для избежания проблем с кодировкой
            String normalizedKeyword = Normalizer.normalize(keyword.trim(), Normalizer.Form.NFKD)
                    .replaceAll("\\p{M}", "");
            logger.debug("Normalized keyword: {}", normalizedKeyword);

            logger.debug("Querying kladrESRepository with keyword: {}", normalizedKeyword);
            List<KladrES> kladrs = kladrESRepository.findByNameContaining(normalizedKeyword);
            logger.debug("Found {} kladrs", kladrs.size());
            logger.debug("Querying streetESRepository with keyword: {}", normalizedKeyword);
            List<StreetES> streets = streetESRepository.findByNameContaining(normalizedKeyword);
            logger.debug("Found {} streets", streets.size());
            logger.debug("Found {} kladrs and {} streets for keyword: {}", kladrs.size(), streets.size(), normalizedKeyword);

            List<String> results = Stream.concat(
                            kladrs.stream().map(k -> new SearchResult(
                                    buildFullPath(k),
                                    socrPriority.getOrDefault(k.getSocr(), 99))),
                            streets.stream().map(s -> new SearchResult(
                                    buildFullPathForStreet(s),
                                    socrPriority.getOrDefault(s.getSocr(), 99)))
                    )
                    .sorted(Comparator.comparingInt(SearchResult::priority))
                    .limit(Math.min(20, limit))
                    .map(SearchResult::formatted)
                    .collect(Collectors.toList());
            logger.debug("Returning {} results for keyword: {}", results.size(), normalizedKeyword);
            return results;
        } catch (Exception e) {
            logger.error("Error searching addresses for keyword: {}", keyword, e);
            return Collections.emptyList();
        }
    }

    private String buildFullPath(KladrES kladr) {
        if (kladr == null && kladr.getCode() != null) {
            logger.warn("Kladr or kladr code is null");
            return "";
        }

        List<String> pathElements = new ArrayList<>();
        pathElements.add(formatAddressPart(kladr.getSocr(), kladr.getName()));

        // Кэшируем регион родительский, если есть
        if (kladr.getCode().length() > 2 && !kladr.getCode().endsWith("0000000")) {
            String regionCode = kladr.getCode().substring(0, 2) + "00000000000";
            logger.debug("Looking for region with code: {}", regionCode);

            Optional<KladrES> region = kladrESRepository.findFirstByCode(regionCode);
            if (region.isPresent()) {
                pathElements.add(0, formatAddressPart(region.get().getSocr(), region.get().getName()));
            }
        }

        return String.join(", ", pathElements);
    }

    private String buildFullPathForStreet(StreetES street) {
        if (street == null || street.getCode() == null) {
            logger.warn("Street or street code is null");
            return "";
        }

        List<String> pathElements = new ArrayList<>();
        pathElements.add(formatAddressPart(street.getSocr(), street.getName()));

        if (street.getCode().length() >= 11) {
            String cityCode = street.getCode().substring(0, 11) + "00";
            logger.debug("Looking for city with code: {}", cityCode);

            Optional<KladrES> city = kladrESRepository.findFirstByCode(cityCode);
            if (city.isPresent()) {
                pathElements.add(0, formatAddressPart(city.get().getSocr(), city.get().getName()));

                String regionCode = cityCode.substring(0, 2) + "0000000";
                logger.debug("Looking for region with code: {}", regionCode);

                Optional<KladrES> region = kladrESRepository.findFirstByCode(regionCode);
                if (region.isPresent()) {
                    pathElements.add(0, formatAddressPart(region.get().getSocr(), region.get().getName()));
                }
            }
        }

        return String.join(", ", pathElements);
    }

    private String formatAddressPart(String socr, String name) {
        return (socr != null ? socr + "." : "") + (name != null ? " " + name : "");
    }

    private record SearchResult(String fullPath, int priority) {
        String formatted() {
            return fullPath;
        }
    }

    private Map<String, Integer> getSocrPriorityMap() {
        if (cachedSocrPriorityMap == null) {
            synchronized (this) {
                if (cachedSocrPriorityMap == null) {
                    cachedSocrPriorityMap = new HashMap<>();
                    List<SocrbaseES> socrList = StreamSupport.stream(socrbaseESRepository.findAll().spliterator(), false)
                            .collect(Collectors.toList());
                    logger.debug("Loaded {} socr entries", socrList.size());
                    socrList.forEach(socr -> {
                        if (socr != null && socr.getScName() != null) {
                            cachedSocrPriorityMap.put(socr.getScName(), socr.getLevel());
                            logger.debug("Added socr: {} with level: {}", socr.getScName(), socr.getLevel());
                        }
                    });
                }
            }
        }
        logger.debug("Returning SocrPriorityMap with {} entries", cachedSocrPriorityMap.size());
        return cachedSocrPriorityMap;
    }

    private Set<String> getSettlementSocrs() {
        if (cachedSettlementSocrs == null) {
            synchronized (this) {
                if (cachedSettlementSocrs == null) {
                    cachedSettlementSocrs = socrbaseESRepository.findByLevel(4).stream()
                            .map(SocrbaseES::getScName)
                            .filter(Objects::nonNull)
                            .map(s -> s.replaceAll("\\.$", "")) // Удаляем завершающую точку
                            .collect(Collectors.toSet());
                    logger.debug("Loaded settlement socrs: {}", cachedSettlementSocrs);
                }
            }
        }
        return cachedSettlementSocrs;
    }

    public List<String> getChildAddressElements(String fullAddress, String keyword) {
        try {
            String[] parts = fullAddress.split(", ");
            if (parts.length == 0) {
                return Collections.emptyList();
            }
            String lastPart;
            if (keyword == null) {
                lastPart = parts[parts.length - 1].trim();
            } else {
                lastPart = parts[parts.length - 2].trim();
            }
            String[] socrAndName = lastPart.split("\\.", 2);
            if (socrAndName.length < 2) {
                return Collections.emptyList();
            }

            String socr = socrAndName[0].trim();
            String name = socrAndName[1].trim();
            // Удаляем завершающую запятую, если она есть
            name = name.replaceAll(",$", "").trim();

            Integer level = getSocrPriorityMap().get(socr);
            if (level == null) {
                logger.error("No level found for socr: {}", socr);
                return Collections.emptyList();
            }
            logger.debug("Processing address level: {} for socr: {}, name: {}", level, socr, name);

            switch (level) {
                case 1: // Область - ищем города
                    return findCitiesInRegion(name, socr, keyword);
                case 4: // Город - ищем улицы
                    return findStreetsInCity(name, socr, keyword);
                case 5: // Улица - ищем дома
                    return findHousesOnStreet(name, socr, parts[parts.length - 2].trim(), keyword);
                default:
                    return Collections.emptyList();
            }
        } catch (Exception e) {
            logger.error("Error getting child addresses for: {}, keyword: {}", fullAddress, keyword, e);
            return Collections.emptyList();
        }
    }

    private List<String> findCitiesInRegion(String regionName, String socr, String keyword) {
        Optional<KladrES> region = kladrESRepository.findByNameAndSocr(regionName, socr);
        if (!region.isPresent()) {
            logger.error("Region not found for name: {}, socr: {}", regionName, socr);
            return Collections.emptyList();
        }

        String regionCodePrefix = region.get().getCode().substring(0, 2);
        logger.debug("Region code prefix: {}", regionCodePrefix);

        Set<String> settlementSocrs = getSettlementSocrs();
        logger.debug("Settlement socrs: {}", settlementSocrs);
        if (settlementSocrs.isEmpty()) {
            logger.error("No settlement socrs found");
            return Collections.emptyList();
        }

        if (keyword != null) {
            String normalizedKeyword = Normalizer.normalize(keyword.trim(), Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
            logger.debug("Searching cities with normalized keyword: {}", normalizedKeyword);
            return kladrESRepository.findByCodeStartingWithAndSocrInAndNameContaining(
                            regionCodePrefix, settlementSocrs, normalizedKeyword
                    ).stream()
                    .sorted(Comparator.comparing((KladrES k) -> k.getSocr()).thenComparing(KladrES::getName))
                    .limit(20)
                    .map(k -> formatAddressPart(k.getSocr(), k.getName()))
                    .collect(Collectors.toList());
        } else {
            logger.debug("Searching all cities in region");
            return kladrESRepository.findByCodeStartingWithAndSocrIn(
                            regionCodePrefix, settlementSocrs
                    ).stream()
                    .sorted(Comparator.comparing((KladrES k) -> k.getSocr()).thenComparing(KladrES::getName))
                    .limit(20)
                    .map(k -> formatAddressPart(k.getSocr(), k.getName()))
                    .collect(Collectors.toList());
        }
    }

    private List<String> findStreetsInCity(String cityName, String socr, String keyword) {
        // Находим город по имени и socr для получения кода
        Optional<KladrES> city = kladrESRepository.findByNameAndSocr(cityName, socr);
        if (!city.isPresent()) {
            logger.error("City not found for name: {}, socr: {}", cityName, socr);
            return Collections.emptyList();
        }

        String cityCodePrefix = city.get().getCode().substring(0, 8); // Берем первые 8 цифр кода (регион + город)
        logger.debug("City code prefix for streets: {}", cityCodePrefix);

        if (keyword != null) {
            String normalizedKeyword = Normalizer.normalize(keyword.trim(), Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
            logger.debug("Searching streets with normalized keyword: {}", normalizedKeyword);
            return streetESRepository.findByCodeStartingWithAndNameContaining(
                            cityCodePrefix, normalizedKeyword
                    ).stream()
                    .sorted(Comparator.comparing(StreetES::getSocr).thenComparing(StreetES::getName))
                    .limit(20)
                    .map(s -> formatAddressPart(s.getSocr(), s.getName()))
                    .collect(Collectors.toList());
        } else {
            logger.debug("Searching all streets in city");
            return streetESRepository.findByCodeStartingWith(cityCodePrefix)
                    .stream()
                    .sorted(Comparator.comparing(StreetES::getSocr).thenComparing(StreetES::getName))
                    .limit(20)
                    .map(s -> formatAddressPart(s.getSocr(), s.getName()))
                    .collect(Collectors.toList());
        }
    }

    private List<String> findHousesOnStreet(String streetName, String socr, String cityPart, String keyword) {
        try {
            String[] citySocrAndName = cityPart.split("\\.", 2);
            if (citySocrAndName.length < 2) {
                return Collections.emptyList();
            }
            String citySocr = citySocrAndName[0].trim();
            String cityName = citySocrAndName[1].trim();
            Optional<KladrES> city = kladrESRepository.findByNameAndSocr(cityName, citySocr);
            if (!city.isPresent()) {
                logger.error("City not found for name: {}, socr: {}", cityName, citySocr);
                return Collections.emptyList();
            }
            String cityCodePrefix = city.get().getCode().substring(0, 10);
            logger.debug("City code prefix for street search: {}", cityCodePrefix);

            String normalizedSocr = socr.replaceAll("\\.$", "").trim().toLowerCase();
            logger.debug("Normalized socr for search: {}", normalizedSocr);

            Optional<StreetES> street = streetESRepository.findByNameAndSocrAndCodeStartingWith(streetName, normalizedSocr, cityCodePrefix);
            if (!street.isPresent()) {
                logger.error("Street not found for name: {}, socr: {}, cityCodePrefix: {}", streetName, normalizedSocr, cityCodePrefix);
                return Collections.emptyList();
            }
            String streetCode = street.get().getCode().substring(0, 15); // Используем полный код улицы
            logger.debug("Found street with code: {}", streetCode);

            if (keyword != null) {
                String normalizedKeyword = Normalizer.normalize(keyword.trim(), Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
                logger.debug("Searching houses with normalized keyword: {}", normalizedKeyword);
                return domaESRepository.findByCodeStartingWithAndNameContaining(streetCode, normalizedKeyword).stream()
                        .limit(20)
                        .map(d -> formatAddressPart(d.getSocr(), d.getName()))
                        .collect(Collectors.toList());
            } else {
                logger.debug("Searching all houses for street code: {}", streetCode);
                return domaESRepository.findByCodeStartingWith(streetCode).stream()
                        .limit(20)
                        .map(d -> formatAddressPart(d.getSocr(), d.getName()))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            logger.error("Error finding houses for street: {}, city: {}", streetName, cityPart, e);
            return Collections.emptyList();
        }
    }
}
