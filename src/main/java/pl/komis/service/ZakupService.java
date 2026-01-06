package pl.komis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.komis.model.*;
import pl.komis.repository.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZakupService {

    private final ZakupRepository zakupRepository;
    private final KlientService klientService;
    private final SamochodService samochodService;
    private final PracownikService pracownikService;
    private final MongoDBFunctionService mongoDBFunctionService;

    public Zakup save(Zakup zakup) {
        return zakupRepository.save(zakup);
    }

    // POPRAWIONA METODA - teraz nalicza premię i aktualizuje klienta
    public String createZakupZSaldem(String samochodId, String klientId, String pracownikId,
                                     Double cenaBazowa, Double wykorzystaneSaldo) {
        try {
            // 1. Pobierz klienta
            Klient klient = klientService.findById(klientId)
                    .orElseThrow(() -> new RuntimeException("Klient nie znaleziony"));

            // 2. Pobierz samochód (dla danych)
            Samochod samochod = (Samochod) samochodService.findById(samochodId);
            if (samochod == null) {
                throw new RuntimeException("Samochód nie znaleziony");
            }

            // 3. Pobierz pracownika (dla danych)
            Pracownik pracownik = pracownikService.findById(pracownikId)
                    .orElseThrow(() -> new RuntimeException("Pracownik nie znaleziony"));

            // 4. Oblicz rabat i premię (na podstawie STARYCH danych klienta)
            Double staryRabatProcent = klient.getProcentPremii() != null ?
                    klient.getProcentPremii() : 0.0;

            Double naliczonaPremia = 0.0;
            if (staryRabatProcent > 0.0) {
                naliczonaPremia = cenaBazowa * (staryRabatProcent / 100.0);
                naliczonaPremia = Math.round(naliczonaPremia * 100.0) / 100.0;
            }

            // 5. Oblicz cenę końcową (po odjęciu wykorzystanego salda)
            Double cenaKoncowa = cenaBazowa - wykorzystaneSaldo;
            cenaKoncowa = Math.round(cenaKoncowa * 100.0) / 100.0;

            // 6. Aktualizuj klienta (ZWIĘKSZ saldo o naliczoną premię, ODEJMIJ wykorzystane saldo)
            // Najpierw odejmij wykorzystane saldo
            if (wykorzystaneSaldo > 0) {
                if (klient.getSaldoPremii() < wykorzystaneSaldo) {
                    throw new RuntimeException("Niewystarczające saldo premii");
                }
                klient.setSaldoPremii(klient.getSaldoPremii() - wykorzystaneSaldo);
            }

            // Następnie dodaj naliczoną premię
            klient.setSaldoPremii(klient.getSaldoPremii() + naliczonaPremia);

            // Zwiększ liczbę zakupów i totalWydane
            klient.setLiczbaZakupow(klient.getLiczbaZakupow() + 1);
            klient.setTotalWydane(klient.getTotalWydane() + cenaKoncowa);

            // Aktualizuj procent premii na podstawie NOWEJ liczby zakupów
            if (klient.getLiczbaZakupow() >= 5) {
                klient.setProcentPremii(15.0);
            } else if (klient.getLiczbaZakupow() >= 3) {
                klient.setProcentPremii(10.0);
            } else if (klient.getLiczbaZakupow() >= 2) {
                klient.setProcentPremii(5.0);
            } else {
                klient.setProcentPremii(0.0);
            }

            // Zapisz zaktualizowanego klienta
            klientService.save(klient);

            // 7. Stwórz zakup z kompletnymi danymi
            Zakup zakup = Zakup.builder()
                    .samochodId(samochodId)
                    .klientId(klientId)
                    .pracownikId(pracownikId)
                    .samochodMarka(samochod.getMarka())
                    .samochodModel(samochod.getModel())
                    .klientImieNazwisko(klient.getImie() + " " + klient.getNazwisko())
                    .pracownikImieNazwisko(pracownik.getImie() + " " + pracownik.getNazwisko())
                    .dataZakupu(LocalDate.now())
                    .cenaBazowa(cenaBazowa)
                    .cenaZakupu(cenaKoncowa)
                    .zastosowanyRabat(staryRabatProcent) // Stary procent premii
                    .naliczonaPremia(naliczonaPremia)     // Naliczona premia
                    .wykorzystaneSaldo(wykorzystaneSaldo)
                    .build();

            Zakup saved = zakupRepository.save(zakup);
            return saved.getId();

        } catch (Exception e) {
            log.error("Błąd przy tworzeniu zakupu z wykorzystaniem salda", e);
            throw new RuntimeException("Błąd przy tworzeniu zakupu: " + e.getMessage(), e);
        }
    }

    // Statystyki z MongoDB
    public Map<String, Object> getStatystykiZakupow() {
        return mongoDBFunctionService.getStatystykiZakupow();
    }

    public Map<String, Object> getStatystykiKlienta(String klientId) {
        Map<String, Object> statystyki = new HashMap<>();

        try {
            List<Zakup> zakupy = zakupRepository.findByKlientId(klientId);

            Double sumaCenaBazowa = zakupy.stream()
                    .map(z -> z.getCenaBazowa() != null ? z.getCenaBazowa() : 0.0)
                    .reduce(0.0, Double::sum);

            Double sumaCenaZakupu = zakupy.stream()
                    .map(z -> z.getCenaZakupu() != null ? z.getCenaZakupu() : 0.0)
                    .reduce(0.0, Double::sum);

            Double sumaWykorzystaneSaldo = zakupy.stream()
                    .map(z -> z.getWykorzystaneSaldo() != null ? z.getWykorzystaneSaldo() : 0.0)
                    .reduce(0.0, Double::sum);

            Double sumaNaliczonaPremia = zakupy.stream()
                    .map(z -> z.getNaliczonaPremia() != null ? z.getNaliczonaPremia() : 0.0)
                    .reduce(0.0, Double::sum);

            statystyki.put("liczbaZakupow", zakupy.size());
            statystyki.put("sumaCenaBazowa", sumaCenaBazowa);
            statystyki.put("sumaCenaZakupu", sumaCenaZakupu);
            statystyki.put("sumaWykorzystaneSaldo", sumaWykorzystaneSaldo);
            statystyki.put("sumaNaliczonaPremia", sumaNaliczonaPremia);

        } catch (Exception e) {
            statystyki.put("error", "Błąd podczas pobierania statystyk: " + e.getMessage());
        }

        return statystyki;
    }

    public List<Map<String, Object>> getMiesieczneStatystyki() {
        List<Map<String, Object>> statystyki = new ArrayList<>();

        try {
            // Przykładowa agregacja miesięczna
            Calendar calendar = Calendar.getInstance();
            List<Zakup> zakupy = zakupRepository.findAll();

            Map<String, Map<String, Object>> miesiaceMap = new HashMap<>();

            for (Zakup zakup : zakupy) {
                if (zakup.getDataZakupu() != null) {
                    // Konwersja LocalDate na Date
                    Date date = Date.from(zakup.getDataZakupu()
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant());
                    calendar.setTime(date);
                    String klucz = calendar.get(Calendar.YEAR) + "-" + (calendar.get(Calendar.MONTH) + 1);

                    miesiaceMap.putIfAbsent(klucz, new HashMap<>());
                    Map<String, Object> miesiac = miesiaceMap.get(klucz);

                    miesiac.put("miesiac", klucz);

                    // Pobierz aktualne wartości
                    Double sumaCena = miesiac.containsKey("sumaCena") ?
                            (Double) miesiac.get("sumaCena") : 0.0;
                    int liczba = miesiac.containsKey("liczbaZakupow") ?
                            (int) miesiac.get("liczbaZakupow") : 0;

                    // Zaktualizuj
                    sumaCena += (zakup.getCenaZakupu() != null ?
                            zakup.getCenaZakupu() : 0.0);
                    liczba++;

                    // Zapisz z powrotem
                    miesiac.put("sumaCena", sumaCena);
                    miesiac.put("liczbaZakupow", liczba);
                }
            }

            // Konwertuj mapę na listę
            statystyki = new ArrayList<>(miesiaceMap.values());

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Błąd podczas pobierania statystyk: " + e.getMessage());
            statystyki.add(error);
        }

        return statystyki;
    }

    // Reszta metod do odczytu
    public List<Zakup> findAll() {
        return zakupRepository.findAll();
    }

    public Optional<Zakup> findById(String id) {
        return zakupRepository.findById(id);
    }

    public List<Zakup> findByKlientId(String klientId) {
        return zakupRepository.findByKlientId(klientId);
    }

    public void remove(String id) {
        zakupRepository.deleteById(id);
    }

    // Proste metody nadal w Javie
    public boolean isSamochodKupiony(String samochodId) {
        return zakupRepository.existsBySamochodId(samochodId);
    }
}