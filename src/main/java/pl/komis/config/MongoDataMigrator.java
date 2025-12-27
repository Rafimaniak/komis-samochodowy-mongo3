package pl.komis.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import pl.komis.model.Klient;
import pl.komis.model.Samochod;
import pl.komis.repository.KlientRepository;
import pl.komis.repository.SamochodRepository;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MongoDataMigrator implements CommandLineRunner {

    private final SamochodRepository samochodRepository;
    private final KlientRepository klientRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) {
        System.out.println("=== SPRAWDZANIE BAZY DANYCH ===");

        // Tylko sprawdź czy dane istnieją, nie importuj ponownie
        long samochodyCount = samochodRepository.count();
        long klienciCount = klientRepository.count();

        System.out.println("Liczba samochodów w bazie: " + samochodyCount);
        System.out.println("Liczba klientów w bazie: " + klienciCount);

        // Jeśli brak danych, dodaj przykładowe
        if (samochodyCount == 0) {
            addSampleCars();
        }

        if (klienciCount == 0) {
            addSampleClients();
        }

        System.out.println("=== BAZA DANYCH GOTOWA ===");
    }

    private void addSampleCars() {
        System.out.println("Dodawanie przykładowych samochodów...");

        List<Samochod> samochody = new ArrayList<>();

        // Samochód 1
        Samochod s1 = new Samochod();
        s1.setMarka("BMW");
        s1.setModel("Seria 3");
        s1.setRokProdukcji(2020);
        s1.setPrzebieg(50000);
        s1.setPojemnoscSilnika(2.0);
        s1.setRodzajPaliwa("Benzyna");
        s1.setSkrzyniaBiegow("Automatyczna");
        s1.setKolor("Czarny");
        s1.setCena(new BigDecimal("120000"));
        s1.setStatus("DOSTEPNY");
        s1.setDataDodania(LocalDate.now());
        s1.setZdjecieUrl("https://example.com/bmw.jpg");
        samochody.add(s1);

        // Samochód 2
        Samochod s2 = new Samochod();
        s2.setMarka("Audi");
        s2.setModel("A4");
        s2.setRokProdukcji(2019);
        s2.setPrzebieg(60000);
        s2.setPojemnoscSilnika(2.0);
        s2.setRodzajPaliwa("Diesel");
        s2.setSkrzyniaBiegow("Manualna");
        s2.setKolor("Biały");
        s2.setCena(new BigDecimal("95000"));
        s2.setStatus("DOSTEPNY");
        s2.setDataDodania(LocalDate.now());
        s2.setZdjecieUrl("https://example.com/audi.jpg");
        samochody.add(s2);

        // Samochód 3
        Samochod s3 = new Samochod();
        s3.setMarka("Toyota");
        s3.setModel("Corolla");
        s3.setRokProdukcji(2020);
        s3.setPrzebieg(45000);
        s3.setPojemnoscSilnika(1.8);
        s3.setRodzajPaliwa("Hybryda");
        s3.setSkrzyniaBiegow("Automatyczna");
        s3.setKolor("Czarny");
        s3.setCena(new BigDecimal("75000"));
        s3.setStatus("DOSTEPNY");
        s3.setDataDodania(LocalDate.now());
        s3.setZdjecieUrl("https://example.com/toyota.jpg");
        samochody.add(s3);

        samochodRepository.saveAll(samochody);
        System.out.println("Dodano " + samochody.size() + " przykładowych samochodów");
    }

    private void addSampleClients() {
        System.out.println("Dodawanie przykładowych klientów...");

        List<Klient> klienci = new ArrayList<>();

        // Klient 1
        Klient k1 = new Klient();
        k1.setImie("Jan");
        k1.setNazwisko("Kowalski");
        k1.setEmail("jan.kowalski@example.com");
        k1.setTelefon("123456789");
        k1.setLiczbaZakupow(2);
        k1.setProcentPremii(new BigDecimal("5"));
        k1.setSaldoPremii(new BigDecimal("1500"));
        k1.setTotalWydane(new BigDecimal("150000"));
        klienci.add(k1);

        // Klient 2
        Klient k2 = new Klient();
        k2.setImie("Anna");
        k2.setNazwisko("Nowak");
        k2.setEmail("anna.nowak@example.com");
        k2.setTelefon("987654321");
        k2.setLiczbaZakupow(0);
        k2.setProcentPremii(BigDecimal.ZERO);
        k2.setSaldoPremii(BigDecimal.ZERO);
        k2.setTotalWydane(BigDecimal.ZERO);
        klienci.add(k2);

        // Klient 3 (klient z historią)
        Klient k3 = new Klient();
        k3.setImie("Piotr");
        k3.setNazwisko("Wiśniewski");
        k3.setEmail("piotr.wisniewski@example.com");
        k3.setTelefon("555666777");
        k3.setLiczbaZakupow(5);
        k3.setProcentPremii(new BigDecimal("15"));
        k3.setSaldoPremii(new BigDecimal("12000"));
        k3.setTotalWydane(new BigDecimal("450000"));
        klienci.add(k3);

        klientRepository.saveAll(klienci);
        System.out.println("Dodano " + klienci.size() + " przykładowych klientów");
    }
}