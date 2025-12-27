package pl.komis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.komis.model.Promocja;
import pl.komis.repository.PromocjaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PromocjaService {

    private final PromocjaRepository promocjaRepository;

    public List<Promocja> findAll() {
        return promocjaRepository.findAll();
    }

    public Optional<Promocja> findById(String id) {
        return promocjaRepository.findById(id);
    }

    public Promocja save(Promocja promocja) {
        return promocjaRepository.save(promocja);
    }

    public void delete(String id) {
        promocjaRepository.deleteById(id);
    }

    // Pobierz wszystkie aktywne promocje
    public List<Promocja> findActivePromotions() {
        LocalDate today = LocalDate.now();
        return promocjaRepository.findByAktywnaTrueAndDataRozpoczeciaBeforeAndDataZakonczeniaAfter(
                today, today);
    }

    // Pobierz promocje według rodzaju
    public List<Promocja> findByRodzaj(String rodzaj) {
        return promocjaRepository.findByRodzaj(rodzaj);
    }

    // Aktywuj/deaktywuj promocję
    public Promocja togglePromotionStatus(String id) {
        Promocja promocja = promocjaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promocja nie znaleziona"));
        promocja.setAktywna(!promocja.getAktywna());
        return promocjaRepository.save(promocja);
    }

    // Pobierz promocje, które są obecnie ważne
    public List<Promocja> findValidPromotions() {
        LocalDate today = LocalDate.now();
        return promocjaRepository.findByDataRozpoczeciaBeforeAndDataZakonczeniaAfter(today, today);
    }

    // Pobierz promocje według statusu aktywności
    public List<Promocja> findByAktywna(boolean aktywna) {
        return promocjaRepository.findByAktywna(aktywna);
    }
}