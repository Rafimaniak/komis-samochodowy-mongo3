package pl.komis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import pl.komis.dto.SearchCriteria;
import pl.komis.model.Samochod;
import pl.komis.repository.SamochodRepository;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SamochodService {

    private final SamochodRepository samochodRepository;
    private final MongoDBFunctionService mongoDBFunctionService;
    private final MongoTemplate mongoTemplate;

    // 1. Użyj funkcji MongoDB do pobrania unikalnych marek
    public List<String> findAllMarki() {
        try {
            // Używamy agregacji MongoDB, aby uzyskać unikalne marki
            Aggregation aggregation = Aggregation.newAggregation(
                    Aggregation.group("marka"),
                    Aggregation.sort(Sort.by("marka").ascending())
            );

            AggregationResults<Map> results = mongoTemplate.aggregate(aggregation, "samochody", Map.class);

            return results.getMappedResults()
                    .stream()
                    .map(map -> (String) map.get("_id"))
                    .filter(marka -> marka != null && !marka.isEmpty())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Błąd pobierania marek: {}", e.getMessage());
            // Fallback: użyj repozytorium
            return samochodRepository.findAll().stream()
                    .map(Samochod::getMarka)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    // 2. Proste wyszukiwanie przez funkcję MongoDB
    public List<Samochod> searchCarsSimple(String marka, String model, String status) {
        Map<String, Object> kryteria = new HashMap<>();

        if (marka != null && !marka.isEmpty()) {
            kryteria.put("marka", marka);
        }
        if (model != null && !model.isEmpty()) {
            kryteria.put("model", model);
        }
        if (status != null && !status.isEmpty()) {
            kryteria.put("status", status);
        }
        // NIE ustawiaj domyślnie "DOSTEPNY" - pozwól na puste (wszystkie statusy)

        return wyszukajSamochody(kryteria);
    }

    // 3. Zaawansowane wyszukiwanie przez funkcję MongoDB
    // W metodzie searchCars() zmień wszystkie gettery:
    public List<Samochod> searchCars(SearchCriteria criteria) {
        Map<String, Object> kryteria = new HashMap<>();

        // Mapowanie wszystkich pól z SearchCriteria do formatu funkcji MongoDB
        if (criteria.getMarka() != null && !criteria.getMarka().isEmpty()) {
            kryteria.put("marka", criteria.getMarka());
        }
        if (criteria.getModel() != null && !criteria.getModel().isEmpty()) {
            kryteria.put("model", criteria.getModel());
        }
        if (criteria.getMinCena() != null) {
            kryteria.put("minCena", criteria.getMinCena());
        }
        if (criteria.getMaxCena() != null) {
            kryteria.put("maxCena", criteria.getMaxCena());
        }
        if (criteria.getMinRok() != null) {
            kryteria.put("minRok", criteria.getMinRok());
        }
        if (criteria.getMaxRok() != null) {
            kryteria.put("maxRok", criteria.getMaxRok());
        }
        if (criteria.getStatus() != null && !criteria.getStatus().isEmpty()) {
            kryteria.put("status", criteria.getStatus());
        }
        if (criteria.getMinPrzebieg() != null) {  // DODAJ: minPrzebieg
            kryteria.put("minPrzebieg", criteria.getMinPrzebieg());
        }
        if (criteria.getMaxPrzebieg() != null) {
            kryteria.put("maxPrzebieg", criteria.getMaxPrzebieg());
        }
        if (criteria.getRodzajPaliwa() != null && !criteria.getRodzajPaliwa().isEmpty()) {
            kryteria.put("rodzajPaliwa", criteria.getRodzajPaliwa());
        }
        if (criteria.getSkrzyniaBiegow() != null && !criteria.getSkrzyniaBiegow().isEmpty()) {
            kryteria.put("skrzyniaBiegow", criteria.getSkrzyniaBiegow());
        }
        if (criteria.getKolor() != null && !criteria.getKolor().isEmpty()) {
            kryteria.put("kolor", criteria.getKolor());
        }
        if (criteria.getLimit() != null) {
            kryteria.put("limit", criteria.getLimit());
        }

        // Wywołaj funkcję MongoDB
        List<Samochod> wyniki = mongoDBFunctionService.wyszukajSamochody(kryteria);

        // ZAWSZE zwracaj listę, nie null
        return wyniki != null ? wyniki : Collections.emptyList();
    }

    // 4. Konwersja wyników z funkcji MongoDB na obiekty Samochod
    private List<Samochod> konwertujWynikiNaSamochody(List<Map<String, Object>> wyniki) {
        return wyniki.stream()
                .map(this::mapToSamochod)
                .collect(Collectors.toList());
    }

    // 5. Metoda pomocnicza do konwersji Map -> Samochod
    private Samochod mapToSamochod(Map<String, Object> mapa) {
        Samochod samochod = new Samochod();

        // PRAWIDŁOWA OBSŁUGA ID
        if (mapa.containsKey("_id")) {
            Object idObj = mapa.get("_id");
            if (idObj instanceof org.bson.types.ObjectId) {
                // Konwertuj ObjectId na String
                samochod.setId(((org.bson.types.ObjectId) idObj).toString());
            } else {
                // Jeśli już jest String, użyj bezpośrednio
                samochod.setId(idObj.toString());
            }
        } else if (mapa.containsKey("id")) {
            samochod.setId(mapa.get("id").toString());
        }

        if (mapa.containsKey("marka")) {
            samochod.setMarka(mapa.get("marka").toString());
        }
        if (mapa.containsKey("model")) {
            samochod.setModel(mapa.get("model").toString());
        }
        if (mapa.containsKey("cena")) {
            Object cena = mapa.get("cena");
            if (cena != null) {
                if (cena instanceof Number) {
                    samochod.setCena(((Number) cena).doubleValue());
                } else if (cena instanceof String) {
                    try {
                        samochod.setCena(Double.parseDouble((String) cena));
                    } catch (NumberFormatException e) {
                        log.warn("Nieprawidłowy format ceny: {}", cena);
                    }
                }
            }
        }
        if (mapa.containsKey("status")) {
            samochod.setStatus(mapa.get("status").toString());
        }
        if (mapa.containsKey("rokProdukcji")) {
            Object rok = mapa.get("rokProdukcji");
            if (rok != null) {
                if (rok instanceof Number) {
                    samochod.setRokProdukcji(((Number) rok).intValue());
                } else if (rok instanceof String) {
                    try {
                        samochod.setRokProdukcji(Integer.parseInt((String) rok));
                    } catch (NumberFormatException e) {
                        log.warn("Nieprawidłowy format roku: {}", rok);
                    }
                }
            }
        }
        if (mapa.containsKey("przebieg")) {
            Object przebieg = mapa.get("przebieg");
            if (przebieg != null) {
                if (przebieg instanceof Number) {
                    samochod.setPrzebieg((int) ((Number) przebieg).doubleValue());
                }
            }
        }

        return samochod;
    }

    // 6. Podstawowe operacje CRUD
    public List<Samochod> findAll() {
        try {
            List<Samochod> result = samochodRepository.findAll();
            // Zawsze zwracaj listę, nie null
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.error("Błąd podczas pobierania samochodów: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public Samochod findById(String id) {
        return samochodRepository.findById(id).orElse(null);
    }

    public Samochod save(Samochod samochod) {
        return samochodRepository.save(samochod);
    }

    public void delete(String id) {
        samochodRepository.deleteById(id);
    }

    public boolean existsById(String id) {
        return samochodRepository.existsById(id);
    }

    public long countAll() {
        try {
            return samochodRepository.count();
        } catch (Exception e) {
            log.error("Błąd podczas zliczania samochodów: {}", e.getMessage());
            return 0;
        }
    }

    public long countByStatus(String status) {
        try {
            // Możesz użyć MongoTemplate
            Query query = new Query(Criteria.where("status").is(status));
            return mongoTemplate.count(query, Samochod.class);

            // LUB jeśli masz metodę w repozytorium:
            // return samochodRepository.countByStatus(status);
        } catch (Exception e) {
            log.error("Błąd podczas zliczania samochodów po statusie '{}': {}", status, e.getMessage());
            return 0;
        }
    }

    public List<Samochod> wyszukajSamochody(Map<String, Object> kryteria) {
        Query query = new Query();

        if (kryteria.containsKey("marka") && kryteria.get("marka") != null && !((String)kryteria.get("marka")).isEmpty()) {
            query.addCriteria(Criteria.where("marka").regex((String)kryteria.get("marka"), "i"));
        }

        if (kryteria.containsKey("model") && kryteria.get("model") != null && !((String)kryteria.get("model")).isEmpty()) {
            query.addCriteria(Criteria.where("model").regex((String)kryteria.get("model"), "i"));
        }

        if (kryteria.containsKey("minCena") && kryteria.get("minCena") != null) {
            query.addCriteria(Criteria.where("cena").gte((Double)kryteria.get("minCena")));
        }

        if (kryteria.containsKey("maxCena") && kryteria.get("maxCena") != null) {
            query.addCriteria(Criteria.where("cena").lte((Double)kryteria.get("maxCena")));
        }

        if (kryteria.containsKey("minRok") && kryteria.get("minRok") != null) {
            query.addCriteria(Criteria.where("rokProdukcji").gte((Integer)kryteria.get("minRok")));
        }

        if (kryteria.containsKey("maxRok") && kryteria.get("maxRok") != null) {
            query.addCriteria(Criteria.where("rokProdukcji").lte((Integer)kryteria.get("maxRok")));
        }

        if (kryteria.containsKey("minPrzebieg") && kryteria.get("minPrzebieg") != null) {
            query.addCriteria(Criteria.where("przebieg").gte((Integer)kryteria.get("minPrzebieg")));
        }

        if (kryteria.containsKey("maxPrzebieg") && kryteria.get("maxPrzebieg") != null) {
            query.addCriteria(Criteria.where("przebieg").lte((Integer)kryteria.get("maxPrzebieg")));
        }

        if (kryteria.containsKey("rodzajPaliwa") && kryteria.get("rodzajPaliwa") != null && !((String)kryteria.get("rodzajPaliwa")).isEmpty()) {
            query.addCriteria(Criteria.where("rodzajPaliwa").is((String)kryteria.get("rodzajPaliwa")));
        }

        if (kryteria.containsKey("skrzyniaBiegow") && kryteria.get("skrzyniaBiegow") != null && !((String)kryteria.get("skrzyniaBiegow")).isEmpty()) {
            query.addCriteria(Criteria.where("skrzyniaBiegow").is((String)kryteria.get("skrzyniaBiegow")));
        }

        // DODAJ: filtrowanie po statusie TYLKO jeżeli jest w kryteriach
        if (kryteria.containsKey("status") && kryteria.get("status") != null && !((String)kryteria.get("status")).isEmpty()) {
            query.addCriteria(Criteria.where("status").is((String)kryteria.get("status")));
        }

        // DODAJ: filtrowanie po kolorze
        if (kryteria.containsKey("kolor") && kryteria.get("kolor") != null && !((String)kryteria.get("kolor")).isEmpty()) {
            query.addCriteria(Criteria.where("kolor").regex((String)kryteria.get("kolor"), "i"));
        }

        // DODAJ: sortowanie po dacie dodania (najnowsze pierwsze)
        query.with(Sort.by(Sort.Direction.DESC, "dataDodania"));

        // Limit wyników
        if (kryteria.containsKey("limit") && kryteria.get("limit") != null) {
            Integer limit = (Integer) kryteria.get("limit");
            query.limit(limit);
        }

        return mongoTemplate.find(query, Samochod.class);
    }

    public List<Map<String, Object>> wyszukajSamochodyJakoMap(Map<String, Object> kryteria) {
        List<Samochod> samochody = wyszukajSamochody(kryteria);
        return konwertujDoMap(samochody);
    }

    private List<Map<String, Object>> konwertujDoMap(List<Samochod> samochody) {
        return samochody.stream()
                .map(this::samochodDoMap)
                .collect(Collectors.toList());
    }

    private Map<String, Object> samochodDoMap(Samochod samochod) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", samochod.getId());
        map.put("marka", samochod.getMarka());
        map.put("model", samochod.getModel());
        map.put("rokProdukcji", samochod.getRokProdukcji());
        map.put("przebieg", samochod.getPrzebieg());
        map.put("pojemnoscSilnika", samochod.getPojemnoscSilnika());
        map.put("rodzajPaliwa", samochod.getRodzajPaliwa());
        map.put("skrzyniaBiegow", samochod.getSkrzyniaBiegow());
        map.put("kolor", samochod.getKolor());
        map.put("cena", samochod.getCena());  // Double
        map.put("status", samochod.getStatus());
        map.put("zdjecieNazwa", samochod.getZdjecieNazwa());
        map.put("zdjecieUrl", samochod.getZdjecieUrl());
        return map;
    }
}