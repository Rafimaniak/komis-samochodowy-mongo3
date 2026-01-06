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
import java.util.Optional;  // DODANO IMPORT
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

    public long countZarezerwowane() {
        return serwisRepository.countByKosztIsNull();
    }

    public long countZakonczone() {
        return serwisRepository.countByKosztIsNotNull();
    }

    public Double getTotalKoszt() {
        return serwisRepository.findZakonczone().stream()
                .map(Serwis::getKoszt)
                .filter(k -> k != null)
                .reduce(0.0, Double::sum);
    }

    public Map<String, Samochod> getSamochodyMap() {
        return samochodService.findAll().stream()
                .collect(Collectors.toMap(Samochod::getId, Function.identity()));
    }

    public Map<String, pl.komis.model.Pracownik> getPracownicyMap() {
        return pracownikService.findAll().stream()
                .collect(Collectors.toMap(pl.komis.model.Pracownik::getId, Function.identity()));
    }

    public void zakonczSerwis(String id, Double koszt) {
        Serwis serwis = serwisRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Serwis nie znaleziony"));
        serwis.setKoszt(koszt);
        serwisRepository.save(serwis);
    }

    public void anulujSerwis(String id) {
        Serwis serwis = serwisRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Serwis nie znaleziony"));
        if (serwis.getKoszt() == null) {
            serwisRepository.delete(serwis);
        } else {
            throw new RuntimeException("Nie można anulować zakończonego serwisu");
        }
    }

    public List<Samochod> getAllSamochody() {
        return samochodService.findAll();
    }

    public List<pl.komis.model.Pracownik> getAllPracownicy() {
        return pracownikService.findAll();
    }

    public Serwis prepareSerwisForDisplay(Serwis serwis) {
        if (serwis == null) return serwis;

        if (serwis.getSamochodId() != null) {
            // BEZ Optional.ifPresent - używamy zwykłego warunku
            Samochod samochod = samochodService.findById(serwis.getSamochodId());
            if (samochod != null) {
                serwis.setSamochodMarka(samochod.getMarka());
                serwis.setSamochodModel(samochod.getModel());
            }
        }

        return serwis;
    }
}