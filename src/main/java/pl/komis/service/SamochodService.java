package pl.komis.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.komis.model.Samochod;
import pl.komis.repository.SamochodRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SamochodService {

    private final SamochodRepository samochodRepository;
    private final MongoTemplate mongoTemplate;

    @Transactional(readOnly = true)
    public List<Samochod> findAll() {
        return samochodRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Samochod> findById(String id) {
        return samochodRepository.findById(id);
    }

    @Transactional
    public Samochod save(Samochod samochod) {
        // GENERUJ NOWE ID JEŚLI NIE MA
        if (samochod.getId() == null || samochod.getId().trim().isEmpty()) {
            samochod.setId(new ObjectId().toString());
        }

        if (samochod.getDataDodania() == null) {
            samochod.setDataDodania(LocalDate.now());
        }
        if (samochod.getRodzajPaliwa() == null) {
            samochod.setRodzajPaliwa("Benzyna");
        }
        if (samochod.getSkrzyniaBiegow() == null) {
            samochod.setSkrzyniaBiegow("Manualna");
        }
        if (samochod.getPojemnoscSilnika() == null) {
            samochod.setPojemnoscSilnika(2.0);
        }
        if (samochod.getStatus() == null) {
            samochod.setStatus("DOSTEPNY");
        }

        String zdjecieUrl = samochod.getZdjecieUrl();
        if (zdjecieUrl == null) {
            zdjecieUrl = "";
            samochod.setZdjecieUrl(zdjecieUrl);
        }

        return samochodRepository.save(samochod);
    }

    @Transactional(readOnly = true)
    public List<Samochod> searchCars(SearchCriteria criteria) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (criteria.getMarka() != null && !criteria.getMarka().trim().isEmpty()) {
            criteriaList.add(Criteria.where("marka").regex(criteria.getMarka(), "i"));
        }

        if (criteria.getModel() != null && !criteria.getModel().trim().isEmpty()) {
            criteriaList.add(Criteria.where("model").regex(criteria.getModel(), "i"));
        }

        if (criteria.getStatus() != null && !criteria.getStatus().trim().isEmpty()) {
            criteriaList.add(Criteria.where("status").is(criteria.getStatus()));
        }

        if (criteria.getMinRok() != null) {
            criteriaList.add(Criteria.where("rokProdukcji").gte(criteria.getMinRok()));
        }

        if (criteria.getMaxRok() != null) {
            criteriaList.add(Criteria.where("rokProdukcji").lte(criteria.getMaxRok()));
        }

        if (criteria.getMinPrzebieg() != null) {
            criteriaList.add(Criteria.where("przebieg").gte(criteria.getMinPrzebieg()));
        }

        if (criteria.getMaxPrzebieg() != null) {
            criteriaList.add(Criteria.where("przebieg").lte(criteria.getMaxPrzebieg()));
        }

        if (criteria.getMinCena() != null) {
            criteriaList.add(Criteria.where("cena").gte(criteria.getMinCena()));
        }

        if (criteria.getMaxCena() != null) {
            criteriaList.add(Criteria.where("cena").lte(criteria.getMaxCena()));
        }

        if (criteria.getRodzajPaliwa() != null && !criteria.getRodzajPaliwa().trim().isEmpty()) {
            criteriaList.add(Criteria.where("rodzajPaliwa").is(criteria.getRodzajPaliwa()));
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        System.out.println("DEBUG: MongoDB Query: " + query.toString());
        List<Samochod> results = mongoTemplate.find(query, Samochod.class);
        System.out.println("DEBUG: Found " + results.size() + " cars");

        return results;
    }

    @Transactional(readOnly = true)
    public List<Samochod> searchCarsSimple(String marka, String model, String status) {
        String searchMarka = (marka != null && !marka.trim().isEmpty()) ? marka : ".*";
        String searchModel = (model != null && !model.trim().isEmpty()) ? model : ".*";
        String searchStatus = (status != null && !status.trim().isEmpty()) ? status : ".*";

        return samochodRepository.searchCarsSimple(searchMarka, searchModel, searchStatus);
    }

    @Transactional(readOnly = true)
    public List<Samochod> findByMarka(String marka) {
        return samochodRepository.findByMarkaIgnoreCase(marka);
    }

    @Transactional(readOnly = true)
    public List<Samochod> findByModel(String model) {
        return samochodRepository.findByModelIgnoreCase(model);
    }

    @Transactional(readOnly = true)
    public List<Samochod> findByStatus(String status) {
        return samochodRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Samochod> findByCenaBetween(BigDecimal min, BigDecimal max) {
        return samochodRepository.findByCenaBetween(min, max);
    }

    @Transactional(readOnly = true)
    public long count() {
        return samochodRepository.count();
    }

    @Transactional(readOnly = true)
    public long countByStatus(String status) {
        return samochodRepository.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Samochod> findAvailableCars() {
        return samochodRepository.findByStatus("DOSTEPNY");
    }

    @Transactional(readOnly = true)
    public List<Samochod> findSoldCars() {
        return samochodRepository.findByStatus("SPRZEDANY");
    }

    @Transactional(readOnly = true)
    public List<Samochod> findReservedCars() {
        return samochodRepository.findByStatus("ZAREZERWOWANY");
    }

    @Transactional(readOnly = true)
    public boolean existsById(String id) {
        return samochodRepository.existsById(id);
    }

    @Transactional
    public void delete(String id) {
        samochodRepository.deleteById(id);
    }

    @Transactional
    public Samochod updateStatus(String id, String newStatus) {
        Samochod samochod = samochodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Samochód nie znaleziony"));
        samochod.setStatus(newStatus);
        return samochodRepository.save(samochod);
    }

    @Transactional(readOnly = true)
    public List<String> findAllMarki() {
        List<Samochod> samochody = samochodRepository.findAll();
        return samochody.stream()
                .map(Samochod::getMarka)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCarStatistics() {
        long total = count();
        long available = countByStatus("DOSTEPNY");
        long sold = countByStatus("SPRZEDANY");
        long reserved = countByStatus("ZAREZERWOWANY");

        return Map.of(
                "total", total,
                "available", available,
                "sold", sold,
                "reserved", reserved
        );
    }

    @Data
    public static class SearchCriteria {
        private String marka;
        private String model;
        private Integer minRok;
        private Integer maxRok;
        private Integer minPrzebieg;
        private Integer maxPrzebieg;
        private BigDecimal minCena;
        private BigDecimal maxCena;
        private String rodzajPaliwa;
        private String status;
    }
}