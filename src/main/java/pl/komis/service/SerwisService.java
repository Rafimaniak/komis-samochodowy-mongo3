package pl.komis.service;

import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import pl.komis.model.Samochod;
import pl.komis.model.Serwis;
import pl.komis.repository.SerwisRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SerwisService {
    private final SerwisRepository serwisRepository;
    private final SamochodService samochodService;
    private final PracownikService pracownikService;

    public List<Serwis> findAll() {
        return serwisRepository.findAll();
    }

    public Optional<Serwis> findById(String id) {
        return serwisRepository.findById(id);
    }

    public Serwis save(Serwis serwis) {
        // GENERUJ NOWE ID JEŚLI NIE MA
        if (serwis.getId() == null || serwis.getId().trim().isEmpty()) {
            serwis.setId(new ObjectId().toString());
        }
        return serwisRepository.save(serwis);
    }

    public void delete(String id) {
        serwisRepository.deleteById(id);
    }

    public List<Serwis> findBySamochodId(String samochodId) {
        return serwisRepository.findBySamochodId(samochodId);
    }

    public List<Serwis> findByPracownikId(String pracownikId) {
        return serwisRepository.findByPracownikId(pracownikId);
    }

    public List<Serwis> findZarezerwowane() {
        return serwisRepository.findZarezerwowane();
    }

    public List<Serwis> findZakonczone() {
        return serwisRepository.findZakonczone();
    }

    // ZMIENIONE: Używamy dedykowanych metod
    public long countZarezerwowane() {
        return serwisRepository.countByKosztIsNull();
    }

    // ZMIENIONE: Używamy dedykowanych metod
    public long countZakonczone() {
        return serwisRepository.countByKosztIsNotNull();
    }

    public BigDecimal getTotalKoszt() {
        return serwisRepository.findZakonczone().stream()
                .map(Serwis::getKoszt)
                .filter(k -> k != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // NOWA METODA: Pobierz mapę samochodów do wyświetlania nazw
    public Map<String, Samochod> getSamochodyMap() {
        return samochodService.findAll().stream()
                .collect(Collectors.toMap(Samochod::getId, Function.identity()));
    }

    // NOWA METODA: Pobierz mapę pracowników do wyświetlania nazw
    public Map<String, pl.komis.model.Pracownik> getPracownicyMap() {
        return pracownikService.findAll().stream()
                .collect(Collectors.toMap(pl.komis.model.Pracownik::getId, Function.identity()));
    }

    // NOWA METODA: Metoda do zakończenia serwisu (ustawienie kosztu)
    public void zakonczSerwis(String id, BigDecimal koszt) {
        Serwis serwis = serwisRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Serwis nie znaleziony"));
        serwis.setKoszt(koszt);
        serwisRepository.save(serwis);
    }

    // NOWA METODA: Metoda do anulowania serwisu (tylko jeśli zarezerwowany)
    public void anulujSerwis(String id) {
        Serwis serwis = serwisRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Serwis nie znaleziony"));
        if (serwis.getKoszt() == null) {
            serwisRepository.delete(serwis);
        } else {
            throw new RuntimeException("Nie można anulować zakończonego serwisu");
        }
    }

    // NOWA METODA: Pobierz wszystkie samochody dla formularza
    public List<Samochod> getAllSamochody() {
        return samochodService.findAll();
    }

    // NOWA METODA: Pobierz wszystkich pracowników dla formularza
    public List<pl.komis.model.Pracownik> getAllPracownicy() {
        return pracownikService.findAll();
    }

    // Metoda pomocnicza do wypełniania danych w kontrolerze
    public Serwis prepareSerwisForDisplay(Serwis serwis) {
        if (serwis == null) return serwis;

        // Jeśli potrzebujesz pełnych obiektów zamiast ID:
        if (serwis.getSamochodId() != null) {
            samochodService.findById(serwis.getSamochodId()).ifPresent(samochod -> {
                // Możesz dodać logikę do wypełnienia danych samochodu
            });
        }

        return serwis;
    }
}