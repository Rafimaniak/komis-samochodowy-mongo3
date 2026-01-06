package pl.komis.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import pl.komis.model.Samochod;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class MongoDBFunctionsInitializer implements CommandLineRunner {

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            log.info("Inicjalizacja funkcji MongoDB...");

            // Zamiast eval, używamy natywnych metod Spring Data MongoDB
            initCzyscRezerwacje();
            initStatystykiZakupow();
            initStatystykiKlientow();

            log.info("Funkcje MongoDB zainicjalizowane pomyślnie");
        } catch (Exception e) {
            log.warn("Błąd podczas ładowania funkcji MongoDB (eval wycofany w MongoDB 6.0+): {}", e.getMessage());
            log.warn("Używamy natywnych metod Spring Data MongoDB zamiast eval");
        }
    }

    private void initCzyscRezerwacje() {
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

            log.info("Wyczyszczono {} przeterminowanych rezerwacji", wyczyszczone);
        } catch (Exception e) {
            log.error("Błąd podczas czyszczenia rezerwacji: {}", e.getMessage());
        }
    }

    private void initStatystykiZakupow() {
        log.info("Inicjalizacja statystyk zakupów...");
        // Statystyki będą realizowane przez serwisy, nie przez eval
    }

    private void initStatystykiKlientow() {
        log.info("Inicjalizacja statystyk klientów...");
        // Statystyki będą realizowane przez serwisy, nie przez eval
    }
}