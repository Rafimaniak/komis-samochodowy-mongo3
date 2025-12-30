package pl.komis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.komis.model.*;
import pl.komis.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ZakupService {

    private final ZakupRepository zakupRepository;
    private final KlientRepository klientRepository;
    private final SamochodService samochodService;
    private final PracownikService pracownikService;
    private final UserService userService;
    private final MongoTemplate mongoTemplate;

    @Transactional
    public Zakup createZakupZSaldem(String samochodId, String userId, String pracownikId,
                                    BigDecimal cenaBazowa, BigDecimal wykorzystaneSaldo) {

        Samochod samochod = samochodService.findById(samochodId)
                .orElseThrow(() -> new RuntimeException("Samochód nie znaleziony"));

        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

        Klient klient = user.getKlient();
        if (klient == null) {
            throw new RuntimeException("Użytkownik nie ma powiązanego klienta");
        }

        Pracownik pracownik = pracownikService.findById(pracownikId)
                .orElseThrow(() -> new RuntimeException("Pracownik nie znaleziony"));

        if (wykorzystaneSaldo != null && wykorzystaneSaldo.compareTo(cenaBazowa) > 0) {
            throw new RuntimeException("Wykorzystane saldo nie może przekraczać ceny samochodu");
        }

        BigDecimal faktycznieWykorzystane = (wykorzystaneSaldo != null) ? wykorzystaneSaldo : BigDecimal.ZERO;
        if (faktycznieWykorzystane.compareTo(BigDecimal.ZERO) > 0) {
            if (klient.getSaldoPremii() == null ||
                    klient.getSaldoPremii().compareTo(faktycznieWykorzystane) < 0) {
                throw new RuntimeException("Niewystarczające saldo premii. Masz: " +
                        klient.getSaldoPremii() + " zł, a potrzebujesz: " +
                        faktycznieWykorzystane + " zł");
            }
        }

        BigDecimal rabatProcent = klient.getProcentPremii() != null ? klient.getProcentPremii() : BigDecimal.ZERO;
        BigDecimal cenaPoRabacie = cenaBazowa;
        if (rabatProcent.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal mnoznikRabatu = BigDecimal.ONE
                    .subtract(rabatProcent.divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP));
            cenaPoRabacie = cenaBazowa.multiply(mnoznikRabatu).setScale(2, BigDecimal.ROUND_HALF_UP);
        }

        BigDecimal cenaOstateczna = cenaPoRabacie.subtract(faktycznieWykorzystane);

        BigDecimal naliczonaPremia = BigDecimal.ZERO;
        if (rabatProcent.compareTo(BigDecimal.ZERO) > 0) {
            naliczonaPremia = cenaBazowa.multiply(rabatProcent)
                    .divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
        }

        samochod.setStatus("SPRZEDANY");
        samochod.setZarezerwowanyPrzez(null);
        samochod.setDataRezerwacji(null);
        samochodService.save(samochod);

        klient.setLiczbaZakupow(klient.getLiczbaZakupow() + 1);
        klient.setTotalWydane(klient.getTotalWydane().add(cenaOstateczna));
        klient.setSaldoPremii(klient.getSaldoPremii().subtract(faktycznieWykorzystane).add(naliczonaPremia));

        if (klient.getLiczbaZakupow() >= 5) {
            klient.setProcentPremii(new BigDecimal("15"));
        } else if (klient.getLiczbaZakupow() >= 3) {
            klient.setProcentPremii(new BigDecimal("10"));
        } else if (klient.getLiczbaZakupow() >= 2) {
            klient.setProcentPremii(new BigDecimal("5"));
        } else {
            klient.setProcentPremii(BigDecimal.ZERO);
        }

        klientRepository.save(klient);

        Zakup zakup = Zakup.builder()
                .samochod(samochod)
                .klient(klient)
                .pracownik(pracownik)
                .dataZakupu(LocalDate.now())
                .cenaBazowa(cenaBazowa)
                .cenaZakupu(cenaOstateczna)
                .zastosowanyRabat(rabatProcent)
                .naliczonaPremia(naliczonaPremia)
                .wykorzystaneSaldo(faktycznieWykorzystane)
                .build();

        return zakupRepository.save(zakup);
    }

    @Transactional(readOnly = true)
    public List<Zakup> findAll() {
        return zakupRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Zakup> findById(String id) {
        return zakupRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Zakup getById(String id) {
        return zakupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Zakup nie znaleziony"));
    }

    @Transactional(readOnly = true)
    public List<Zakup> findByKlientId(String klientId) {
        System.out.println("DEBUG: Szukam zakupów dla klienta ID: " + klientId);

        if (klientId == null || klientId.trim().isEmpty()) {
            System.out.println("DEBUG: klientId jest null lub pusty");
            return Collections.emptyList();
        }

        try {
            // Spróbuj obu metod
            List<Zakup> zakupy = null;

            // Metoda 1: z @Query
            try {
                zakupy = zakupRepository.findByKlientId(klientId);
                System.out.println("DEBUG: Metoda @Query: znaleziono " + zakupy.size() + " zakupów");
            } catch (Exception e) {
                System.err.println("DEBUG: Błąd metody @Query: " + e.getMessage());
            }

            // Metoda 2: bez @Query (jeśli pierwsza nie działa)
            if (zakupy == null || zakupy.isEmpty()) {
                try {
                    zakupy = zakupRepository.findByKlient_Id(klientId);
                    System.out.println("DEBUG: Metoda findByKlient_Id: znaleziono " + zakupy.size() + " zakupów");
                } catch (Exception e) {
                    System.err.println("DEBUG: Błąd metody findByKlient_Id: " + e.getMessage());
                }
            }

            // Metoda 3: ręczne zapytanie przez MongoTemplate
            if (zakupy == null || zakupy.isEmpty()) {
                try {
                    Query query = new Query(Criteria.where("klient.$id").is(klientId));
                    zakupy = mongoTemplate.find(query, Zakup.class);
                    System.out.println("DEBUG: Metoda MongoTemplate: znaleziono " + zakupy.size() + " zakupów");
                } catch (Exception e) {
                    System.err.println("DEBUG: Błąd metody MongoTemplate: " + e.getMessage());
                }
            }

            if (zakupy == null) {
                zakupy = Collections.emptyList();
            }

            System.out.println("DEBUG: Łącznie znaleziono " + zakupy.size() + " zakupów");

            // Dla debugowania - wypisz szczegóły
            for (Zakup zakup : zakupy) {
                System.out.println("Zakup ID: " + zakup.getId() +
                        ", Data: " + zakup.getDataZakupu() +
                        ", Samochód: " + (zakup.getSamochod() != null ?
                        zakup.getSamochod().getMarka() + " " + zakup.getSamochod().getModel() : "null") +
                        ", Cena: " + zakup.getCenaZakupu());
            }

            return zakupy;

        } catch (Exception e) {
            System.err.println("Błąd w findByKlientId: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    @Transactional(readOnly = true)
    public List<Zakup> findByPracownikId(String id) {
        return zakupRepository.findByPracownikId(id);
    }

    @Transactional(readOnly = true)
    public List<Zakup> findByDateRange(LocalDate od, LocalDate do_) {
        return zakupRepository.findByDataZakupuBetween(od, do_);
    }

    @Transactional
    public Zakup save(Zakup zakup) {
        return zakupRepository.save(zakup);
    }

    @Transactional
    public void remove(String id) {
        try {
            zakupRepository.deleteById(id);
        } catch (Exception e) {
            throw new RuntimeException("Błąd podczas usuwania zakupu: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void delete(String id) {
        remove(id);
    }

    @Transactional(readOnly = true)
    public BigDecimal pobierzSaldoKlienta(String klientId) {
        Klient klient = klientRepository.findById(klientId)
                .orElseThrow(() -> new RuntimeException("Klient nie znaleziony"));
        return klient.getSaldoPremii();
    }

    @Transactional(readOnly = true)
    public boolean czyMozeWykorzystacSaldo(String klientId, BigDecimal kwota) {
        if (kwota == null || kwota.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        Klient klient = klientRepository.findById(klientId)
                .orElseThrow(() -> new RuntimeException("Klient nie znaleziony"));

        return klient.getSaldoPremii() != null &&
                klient.getSaldoPremii().compareTo(kwota) >= 0;
    }

    @Transactional(readOnly = true)
    public BigDecimal getSumaWydatkowKlienta(String klientId) {
        List<Zakup> zakupy = findByKlientId(klientId);
        return zakupy.stream()
                .map(Zakup::getCenaZakupu)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public BigDecimal getSumaWykorzystanegoSaldaKlienta(String klientId) {
        List<Zakup> zakupy = findByKlientId(klientId);
        return zakupy.stream()
                .map(Zakup::getWykorzystaneSaldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public int getLiczbaZakupowKlienta(String klientId) {
        List<Zakup> zakupy = findByKlientId(klientId);
        return zakupy.size();
    }

    @Transactional(readOnly = true)
    public boolean isSamochodKupiony(String samochodId) {
        return zakupRepository.existsBySamochodId(samochodId);
    }

    @Transactional(readOnly = true)
    public boolean isSamochodKupionyPrzezKlienta(String samochodId, String klientId) {
        return zakupRepository.existsBySamochodIdAndKlientId(samochodId, klientId);
    }

    // METODA POMOCNICZA DO ŁADOWANIA ZAKUPÓW Z REFERENCJAMI
    @Transactional(readOnly = true)
    public List<Zakup> findZakupyKlientaZReferencjami(String klientId) {
        List<Zakup> zakupy = zakupRepository.findByKlientId(klientId);

        // Załaduj referencje dla każdego zakupu
        for (Zakup zakup : zakupy) {
            if (zakup.getSamochod() != null && zakup.getSamochod().getId() != null) {
                // Pobierz pełny obiekt samochodu
                Samochod samochod = samochodService.findById(zakup.getSamochod().getId()).orElse(null);
                zakup.setSamochod(samochod);
            }
        }

        return zakupy;
    }
}