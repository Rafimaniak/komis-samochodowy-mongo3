package pl.komis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import pl.komis.model.Klient;
import pl.komis.model.Samochod;
import pl.komis.model.Zakup;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MongoDBFunctionService {

    private final MongoTemplate mongoTemplate;

    public Map<String, Object> getStatystykiZakupow() {
        Map<String, Object> statystyki = new HashMap<>();

        try {
            // Ilość zakupów
            long liczbaZakupow = mongoTemplate.count(new Query(), Zakup.class);
            statystyki.put("liczbaZakupow", liczbaZakupow);

            // Suma kwot
            List<Zakup> zakupy = mongoTemplate.findAll(Zakup.class);
            Double sumaCen = zakupy.stream()
                    .map(Zakup::getCenaZakupu)
                    .filter(Objects::nonNull)
                    .reduce(0.0, Double::sum);
            statystyki.put("sumaCen", sumaCen);

            // Średnia cena
            Double sredniaCena = liczbaZakupow > 0
                    ? sumaCen / liczbaZakupow
                    : 0.0;
            // Zaokrąglamy do 2 miejsc po przecinku
            sredniaCena = Math.round(sredniaCena * 100.0) / 100.0;
            statystyki.put("sredniaCena", sredniaCena);

            // Ostatni zakup
            if (!zakupy.isEmpty()) {
                Zakup ostatniZakup = zakupy.stream()
                        .max(Comparator.comparing(Zakup::getDataZakupu))
                        .orElse(null);
                statystyki.put("ostatniZakup", ostatniZakup);
            }

        } catch (Exception e) {
            log.error("Błąd podczas pobierania statystyk zakupów: {}", e.getMessage());
        }

        return statystyki;
    }

    public Map<String, Object> getStatystykiKlientow() {
        Map<String, Object> statystyki = new HashMap<>();

        try {
            List<Klient> klienci = mongoTemplate.findAll(Klient.class);

            long liczbaKlientow = klienci.size();
            statystyki.put("liczbaKlientow", liczbaKlientow);

            long klienciZPremia = klienci.stream()
                    .filter(k -> k.getProcentPremii() != null && k.getProcentPremii() > 0)
                    .count();
            statystyki.put("klienciZPremia", klienciZPremia);

            Double sumaSaldo = klienci.stream()
                    .map(Klient::getSaldoPremii)
                    .filter(Objects::nonNull)
                    .reduce(0.0, Double::sum);
            statystyki.put("sumaSaldo", sumaSaldo);

            Double sumaWydane = klienci.stream()
                    .map(Klient::getTotalWydane)
                    .filter(Objects::nonNull)
                    .reduce(0.0, Double::sum);
            statystyki.put("sumaWydane", sumaWydane);

        } catch (Exception e) {
            log.error("Błąd podczas pobierania statystyk klientów: {}", e.getMessage());
        }

        return statystyki;
    }

    public List<Map<String, Object>> getTopKlientow(int limit) {
        List<Map<String, Object>> topKlienci = new ArrayList<>();

        try {
            // Pobierz wszystkich klientów i posortuj po totalWydane
            List<Klient> klienci = mongoTemplate.findAll(Klient.class);

            klienci.sort((k1, k2) -> {
                Double w1 = k1.getTotalWydane() != null ? k1.getTotalWydane() : 0.0;
                Double w2 = k2.getTotalWydane() != null ? k2.getTotalWydane() : 0.0;
                return Double.compare(w2, w1); // malejąco
            });

            // Ogranicz do limitu
            List<Klient> ograniczeni = klienci.stream()
                    .limit(limit)
                    .toList();

            for (Klient klient : ograniczeni) {
                Map<String, Object> klientInfo = new HashMap<>();
                klientInfo.put("imie", klient.getImie());
                klientInfo.put("nazwisko", klient.getNazwisko());
                klientInfo.put("liczbaZakupow", klient.getLiczbaZakupow());
                klientInfo.put("totalWydane", klient.getTotalWydane());
                klientInfo.put("procentPremii", klient.getProcentPremii());
                klientInfo.put("saldoPremii", klient.getSaldoPremii());

                topKlienci.add(klientInfo);
            }

        } catch (Exception e) {
            log.error("Błąd podczas pobierania top klientów: {}", e.getMessage());
        }

        return topKlienci;
    }

    public String czyscPrzeterminowaneRezerwacje() {
        try {
            log.info("Czyszczenie przeterminowanych rezerwacji...");

            List<Samochod> zarezerwowaneSamochody = mongoTemplate.find(
                    Query.query(Criteria.where("status").is("ZAREZERWOWANY")
                            .and("dataRezerwacji").exists(true)),
                    Samochod.class
            );

            int wyczyszczone = 0;
            for (Samochod samochod : zarezerwowaneSamochody) {
                if (samochod.getDataRezerwacji() != null) {
                    long dniOdRezerwacji = ChronoUnit.DAYS.between(
                            samochod.getDataRezerwacji(),
                            LocalDate.now()
                    );

                    if (dniOdRezerwacji > 7) {
                        samochod.setStatus("DOSTEPNY");
                        samochod.setZarezerwowanyPrzezKlientId(null);
                        samochod.setDataRezerwacji(null);

                        mongoTemplate.save(samochod);
                        wyczyszczone++;
                    }
                }
            }

            String result = "Wyczyszczono " + wyczyszczone + " przeterminowanych rezerwacji";
            log.info(result);
            return result;

        } catch (Exception e) {
            String error = "Błąd podczas czyszczenia rezerwacji: " + e.getMessage();
            log.error(error);
            return error;
        }
    }

    public Map<String, Object> getMiesieczneStatystyki() {
        Map<String, Object> statystyki = new HashMap<>();

        try {
            // Przykładowa agregacja (dostosuj do swojej struktury Zakup)
            // Aggregation aggregation = Aggregation.newAggregation(
            //     Aggregation.group("miesiąc")
            //         .sum("cenaZakupu").as("suma")
            //         .count().as("ilosc")
            // );

            // AggregationResults<Map> results = mongoTemplate.aggregate(
            //     aggregation, "zakupy", Map.class
            // );

            // Tymczasowo zwracamy puste statystyki
            statystyki.put("miesiace", new ArrayList<>());

        } catch (Exception e) {
            log.error("Błąd podczas pobierania miesięcznych statystyk: {}", e.getMessage());
        }

        return statystyki;
    }

    public List<Map<String, Object>> getTopKlienci(int limit) {
        return getTopKlientow(limit); // użyj istniejącej metody pod inną nazwą
    }

    public List<Samochod> wyszukajSamochody(Map<String, Object> kryteria) {
        try {
            Query query = new Query();

            // Marka
            if (kryteria.containsKey("marka") && kryteria.get("marka") != null && !((String) kryteria.get("marka")).isEmpty()) {
                String marka = (String) kryteria.get("marka");
                query.addCriteria(Criteria.where("marka").regex(marka, "i"));
            }

            // Model
            if (kryteria.containsKey("model") && kryteria.get("model") != null && !((String) kryteria.get("model")).isEmpty()) {
                String model = (String) kryteria.get("model");
                query.addCriteria(Criteria.where("model").regex(model, "i"));
            }

            // Cena od
            if (kryteria.containsKey("minCena") && kryteria.get("minCena") != null) {
                Double minCena = (Double) kryteria.get("minCena");
                query.addCriteria(Criteria.where("cena").gte(minCena));
            }

            // Cena do
            if (kryteria.containsKey("maxCena") && kryteria.get("maxCena") != null) {
                Double maxCena = (Double) kryteria.get("maxCena");
                query.addCriteria(Criteria.where("cena").lte(maxCena));
            }

            // Rok od
            if (kryteria.containsKey("minRok") && kryteria.get("minRok") != null) {
                Integer minRok = (Integer) kryteria.get("minRok");
                query.addCriteria(Criteria.where("rokProdukcji").gte(minRok));
            }

            // Rok do
            if (kryteria.containsKey("maxRok") && kryteria.get("maxRok") != null) {
                Integer maxRok = (Integer) kryteria.get("maxRok");
                query.addCriteria(Criteria.where("rokProdukcji").lte(maxRok));
            }

            // Przebieg od
            if (kryteria.containsKey("minPrzebieg") && kryteria.get("minPrzebieg") != null) {
                Integer minPrzebieg = (Integer) kryteria.get("minPrzebieg");
                query.addCriteria(Criteria.where("przebieg").gte(minPrzebieg));
            }

            // Przebieg do
            if (kryteria.containsKey("maxPrzebieg") && kryteria.get("maxPrzebieg") != null) {
                Integer maxPrzebieg = (Integer) kryteria.get("maxPrzebieg");
                query.addCriteria(Criteria.where("przebieg").lte(maxPrzebieg));
            }

            // Rodzaj paliwa
            if (kryteria.containsKey("rodzajPaliwa") && kryteria.get("rodzajPaliwa") != null && !((String) kryteria.get("rodzajPaliwa")).isEmpty()) {
                String rodzajPaliwa = (String) kryteria.get("rodzajPaliwa");
                query.addCriteria(Criteria.where("rodzajPaliwa").is(rodzajPaliwa));
            }

            // Skrzynia biegów
            if (kryteria.containsKey("skrzyniaBiegow") && kryteria.get("skrzyniaBiegow") != null && !((String) kryteria.get("skrzyniaBiegow")).isEmpty()) {
                String skrzyniaBiegow = (String) kryteria.get("skrzyniaBiegow");
                query.addCriteria(Criteria.where("skrzyniaBiegow").is(skrzyniaBiegow));
            }

            // Status
            if (kryteria.containsKey("status") && kryteria.get("status") != null && !((String) kryteria.get("status")).isEmpty()) {
                String status = (String) kryteria.get("status");
                query.addCriteria(Criteria.where("status").is(status));
            }

            // Kolor - DODANE
            if (kryteria.containsKey("kolor") && kryteria.get("kolor") != null && !((String) kryteria.get("kolor")).isEmpty()) {
                String kolor = (String) kryteria.get("kolor");
                query.addCriteria(Criteria.where("kolor").regex(kolor, "i")); // wyszukiwanie bez uwzględniania wielkości liter
            }

            // Limit wyników - DODANE
            if (kryteria.containsKey("limit") && kryteria.get("limit") != null) {
                Integer limit = (Integer) kryteria.get("limit");
                query.limit(limit);
            }

            return mongoTemplate.find(query, Samochod.class);

        } catch (Exception e) {
            log.error("Błąd podczas wyszukiwania samochodów: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}