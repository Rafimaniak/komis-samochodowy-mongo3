package pl.komis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.stereotype.Service;
import pl.komis.dto.SearchCriteria;
import pl.komis.model.Samochod;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final MongoTemplate mongoTemplate;

    public List<Samochod> fullTextSearch(String query, int limit) {
        TextCriteria criteria = TextCriteria.forDefaultLanguage()
                .matching(query);

        Query mongoQuery = TextQuery.queryText(criteria)
                .sortByScore()
                .with(Sort.by("score"))
                .limit(limit);

        return mongoTemplate.find(mongoQuery, Samochod.class);
    }

    public List<Samochod> advancedSearch(Map<String, Object> filters) {
        Criteria criteria = new Criteria();
        List<Criteria> criteriaList = new ArrayList<>();

        if (filters.containsKey("text")) {
            criteriaList.add(Criteria.byExample(TextCriteria.forDefaultLanguage()
                    .matching((String) filters.get("text"))
                    .getCriteriaObject()));
        }

        if (filters.containsKey("marka")) {
            criteriaList.add(Criteria.where("marka")
                    .is(filters.get("marka").toString()));
        }

        if (filters.containsKey("model")) {
            criteriaList.add(Criteria.where("model")
                    .regex(filters.get("model").toString(), "i"));
        }

        if (filters.containsKey("minCena")) {
            criteriaList.add(Criteria.where("cena")
                    .gte(new BigDecimal(filters.get("minCena").toString())));
        }

        if (filters.containsKey("maxCena")) {
            criteriaList.add(Criteria.where("cena")
                    .lte(new BigDecimal(filters.get("maxCena").toString())));
        }

        if (filters.containsKey("minRok")) {
            criteriaList.add(Criteria.where("rokProdukcji")
                    .gte(Integer.parseInt(filters.get("minRok").toString())));
        }

        if (filters.containsKey("maxRok")) {
            criteriaList.add(Criteria.where("rokProdukcji")
                    .lte(Integer.parseInt(filters.get("maxRok").toString())));
        }

        if (filters.containsKey("status")) {
            criteriaList.add(Criteria.where("status")
                    .is(filters.get("status").toString()));
        }

        if (filters.containsKey("rodzajPaliwa")) {
            criteriaList.add(Criteria.where("rodzajPaliwa")
                    .is(filters.get("rodzajPaliwa").toString()));
        }

        if (filters.containsKey("skrzyniaBiegow")) {
            criteriaList.add(Criteria.where("skrzyniaBiegow")
                    .is(filters.get("skrzyniaBiegow").toString()));
        }

        if (!criteriaList.isEmpty()) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }

        Query query = new Query(criteria)
                .with(Sort.by("cena").ascending());

        return mongoTemplate.find(query, Samochod.class);
    }
    // W metodzie search() zmień wszystkie gettery:
    public List<Samochod> search(SearchCriteria criteria) {
        Map<String, Object> filters = new HashMap<>();

        if (criteria.getMarka() != null && !criteria.getMarka().isEmpty()) {
            filters.put("marka", criteria.getMarka());
        }
        if (criteria.getModel() != null && !criteria.getModel().isEmpty()) {
            filters.put("model", criteria.getModel());
        }
        if (criteria.getMinCena() != null) {  // ZMIANA: getCenaMin() -> getMinCena()
            filters.put("minCena", criteria.getMinCena());
        }
        if (criteria.getMaxCena() != null) {  // ZMIANA: getCenaMax() -> getMaxCena()
            filters.put("maxCena", criteria.getMaxCena());
        }
        if (criteria.getMinRok() != null) {  // ZMIANA: getRokMin() -> getMinRok()
            filters.put("minRok", criteria.getMinRok());
        }
        if (criteria.getMaxRok() != null) {  // ZMIANA: getRokMax() -> getMaxRok()
            filters.put("maxRok", criteria.getMaxRok());
        }
        if (criteria.getStatus() != null && !criteria.getStatus().isEmpty()) {
            filters.put("status", criteria.getStatus());
        }
        if (criteria.getRodzajPaliwa() != null && !criteria.getRodzajPaliwa().isEmpty()) {
            filters.put("rodzajPaliwa", criteria.getRodzajPaliwa());
        }
        if (criteria.getSkrzyniaBiegow() != null && !criteria.getSkrzyniaBiegow().isEmpty()) {
            filters.put("skrzyniaBiegow", criteria.getSkrzyniaBiegow());
        }

        return advancedSearch(filters);
    }
}
