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

        long samochodyCount = samochodRepository.count();
        long klienciCount = klientRepository.count();

        System.out.println("Liczba samochodów w bazie: " + samochodyCount);
        System.out.println("Liczba klientów w bazie: " + klienciCount);

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
        s1.setCena(120000.0);
        s1.setStatus("DOSTEPNY");
        s1.setDataDodania(LocalDate.now());
        s1.setZdjecieNazwa(null);
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
        s2.setCena(95000.0);
        s2.setStatus("DOSTEPNY");
        s2.setDataDodania(LocalDate.now());
        s2.setZdjecieNazwa(null);
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
        s3.setCena(75000.0);
        s3.setStatus("DOSTEPNY");
        s3.setDataDodania(LocalDate.now());
        s3.setZdjecieNazwa(null);
        samochody.add(s3);

        // Dodajemy więcej przykładowych samochodów dla różnorodności
        Samochod s4 = new Samochod();
        s4.setMarka("Volkswagen");
        s4.setModel("Golf");
        s4.setRokProdukcji(2018);
        s4.setPrzebieg(80000);
        s4.setPojemnoscSilnika(1.6);
        s4.setRodzajPaliwa("Diesel");
        s4.setSkrzyniaBiegow("Manualna");
        s4.setKolor("Srebrny");
        s4.setCena(55000.0);
        s4.setStatus("DOSTEPNY");
        s4.setDataDodania(LocalDate.now());
        s4.setZdjecieNazwa(null);
        samochody.add(s4);

        Samochod s5 = new Samochod();
        s5.setMarka("Mercedes");
        s5.setModel("C-Klasa");
        s5.setRokProdukcji(2021);
        s5.setPrzebieg(30000);
        s5.setPojemnoscSilnika(2.0);
        s5.setRodzajPaliwa("Benzyna");
        s5.setSkrzyniaBiegow("Automatyczna");
        s5.setKolor("Niebieski");
        s5.setCena(140000.0);
        s5.setStatus("ZAREZERWOWANY");
        s5.setDataDodania(LocalDate.now());
        s5.setZarezerwowanyPrzezKlientId("sample_client_1");
        s5.setDataRezerwacji(LocalDate.now().minusDays(3));
        s5.setZdjecieNazwa(null);
        samochody.add(s5);

        Samochod s6 = new Samochod();
        s6.setMarka("Ford");
        s6.setModel("Focus");
        s6.setRokProdukcji(2017);
        s6.setPrzebieg(95000);
        s6.setPojemnoscSilnika(1.5);
        s6.setRodzajPaliwa("Benzyna");
        s6.setSkrzyniaBiegow("Manualna");
        s6.setKolor("Czerwony");
        s6.setCena(45000.0);
        s6.setStatus("SPRZEDANY");
        s6.setDataDodania(LocalDate.now());
        s6.setZdjecieNazwa(null);
        samochody.add(s6);

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
        k1.setProcentPremii(5.0);  // Double
        k1.setSaldoPremii(1500.0);  // Double
        k1.setTotalWydane(150000.0);  // Double
        klienci.add(k1);

        // Klient 2
        Klient k2 = new Klient();
        k2.setImie("Anna");
        k2.setNazwisko("Nowak");
        k2.setEmail("anna.nowak@example.com");
        k2.setTelefon("987654321");
        k2.setLiczbaZakupow(0);
        k2.setProcentPremii(0.0);  // Double
        k2.setSaldoPremii(0.0);  // Double
        k2.setTotalWydane(0.0);  // Double
        klienci.add(k2);

        // Klient 3 (klient z historią)
        Klient k3 = new Klient();
        k3.setImie("Piotr");
        k3.setNazwisko("Wiśniewski");
        k3.setEmail("piotr.wisniewski@example.com");
        k3.setTelefon("555666777");
        k3.setLiczbaZakupow(5);
        k3.setProcentPremii(15.0);  // Double
        k3.setSaldoPremii(12000.0);  // Double
        k3.setTotalWydane(450000.0);  // Double
        klienci.add(k3);

        // Dodajemy więcej klientów dla testów
        Klient k4 = new Klient();
        k4.setImie("Maria");
        k4.setNazwisko("Kowalczyk");
        k4.setEmail("maria.kowalczyk@example.com");
        k4.setTelefon("111222333");
        k4.setLiczbaZakupow(1);
        k4.setProcentPremii(0.0);  // Double
        k4.setSaldoPremii(500.0);  // Double
        k4.setTotalWydane(75000.0);  // Double
        klienci.add(k4);

        Klient k5 = new Klient();
        k5.setImie("Andrzej");
        k5.setNazwisko("Zieliński");
        k5.setEmail("andrzej.zielinski@example.com");
        k5.setTelefon("444555666");
        k5.setLiczbaZakupow(3);
        k5.setProcentPremii(10.0);  // Double
        k5.setSaldoPremii(3000.0);  // Double
        k5.setTotalWydane(280000.0);  // Double
        klienci.add(k5);

        klientRepository.saveAll(klienci);
        System.out.println("Dodano " + klienci.size() + " przykładowych klientów");
    }
}