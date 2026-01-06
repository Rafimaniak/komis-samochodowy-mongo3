package pl.komis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import pl.komis.model.Samochod;
import pl.komis.model.Zakup;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AgregacjeService {

    private final MongoTemplate mongoTemplate;

    // Statystyki sprzedaży miesięczne
    public List<Map> getMiesieczneStatystyki(int rok) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("dataZakupu")
                        .gte(LocalDate.of(rok, 1, 1))
                        .lte(LocalDate.of(rok, 12, 31))),
                Aggregation.project()
                        .andExpression("month(dataZakupu)").as("miesiac")
                        .andExpression("year(dataZakupu)").as("rok")
                        .and("cenaZakupu").as("cena"),
                Aggregation.group("rok", "miesiac")
                        .count().as("liczbaZakupow")
                        .sum("cena").as("sumaCena")
                        .avg("cena").as("sredniaCena"),
                Aggregation.sort(org.springframework.data.domain.Sort.by("rok").descending()
                        .and(org.springframework.data.domain.Sort.by("miesiac").descending()))
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "zakupy", Map.class
        );

        return results.getMappedResults();
    }

    // Top klienci
    public List<Map> getTopKlienci(int limit) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.group("klient_id")
                        .first("klient_imie_nazwisko").as("klientNazwa")
                        .count().as("liczbaZakupow")
                        .sum("cenaZakupu").as("totalWydane")
                        .avg("cenaZakupu").as("sredniaCena"),
                Aggregation.sort(org.springframework.data.domain.Sort.by("totalWydane").descending()),
                Aggregation.limit(limit)
        );

        return mongoTemplate.aggregate(aggregation, "zakupy", Map.class)
                .getMappedResults();
    }

    // Statystyki marki
    public List<Map> getStatystykiMarki() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.group("samochod_marka", "samochod_model")
                        .count().as("liczbaSprzedanych")
                        .sum("cenaZakupu").as("sumaPrzychodow")
                        .avg("cenaZakupu").as("sredniaCena"),
                Aggregation.sort(org.springframework.data.domain.Sort.by("liczbaSprzedanych").descending())
        );

        return mongoTemplate.aggregate(aggregation, "zakupy", Map.class)
                .getMappedResults();
    }

    // Dostępne samochody z agregacją
    public List<Samochod> getDostepneSamochodyZAgregacja() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("status").is("DOSTEPNY")),
                Aggregation.sort(org.springframework.data.domain.Sort.by("marka").ascending()
                        .and(org.springframework.data.domain.Sort.by("model").ascending()))
        );

        return mongoTemplate.aggregate(aggregation, "samochody", Samochod.class)
                .getMappedResults();
    }
}