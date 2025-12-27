package pl.komis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.komis.model.KlientPromocja;
import pl.komis.repository.KlientPromocjaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KlientPromocjaService {

    private final KlientPromocjaRepository klientPromocjaRepository;

    public List<KlientPromocja> findAll() {
        return klientPromocjaRepository.findAll();
    }

    public Optional<KlientPromocja> findById(String id) {
        return klientPromocjaRepository.findById(id);
    }

    public KlientPromocja save(KlientPromocja klientPromocja) {
        return klientPromocjaRepository.save(klientPromocja);
    }

    public void delete(String id) {
        klientPromocjaRepository.deleteById(id);
    }

    // Metoda pomocnicza do dodania promocji dla klienta
    public KlientPromocja przypiszPromocjeKlientowi(String klientId, String promocjaId) {
        KlientPromocja klientPromocja = KlientPromocja.builder()
                .klientId(klientId)
                .promocjaId(promocjaId)
                .dataPrzyznania(LocalDate.now())
                .wykorzystana(false)
                .build();

        return klientPromocjaRepository.save(klientPromocja);
    }

    // Pobierz promocje dla klienta
    public List<KlientPromocja> getPromocjeForKlient(String klientId) {
        return klientPromocjaRepository.findByKlientId(klientId);
    }

    // Pobierz klientów z daną promocją
    public List<KlientPromocja> getKlienciWithPromocja(String promocjaId) {
        return klientPromocjaRepository.findByPromocjaId(promocjaId);
    }

    // Sprawdź czy klient ma już daną promocję
    public boolean czyKlientMaPromocje(String klientId, String promocjaId) {
        return klientPromocjaRepository.existsByKlientIdAndPromocjaId(klientId, promocjaId);
    }

    // Usuń promocję dla klienta
    public void usunPromocjeDlaKlienta(String klientId, String promocjaId) {
        KlientPromocja klientPromocja = klientPromocjaRepository.findByKlientIdAndPromocjaId(klientId, promocjaId);
        if (klientPromocja != null) {
            klientPromocjaRepository.delete(klientPromocja);
        }
    }
}