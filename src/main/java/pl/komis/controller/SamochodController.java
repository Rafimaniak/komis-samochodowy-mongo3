package pl.komis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.komis.model.*;
import pl.komis.service.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/samochody")
@RequiredArgsConstructor
public class SamochodController {

    private final SamochodService samochodService;
    private final UserService userService;
    private final KlientService klientService;
    private final PracownikService pracownikService;
    private final ZakupService zakupService;

    // Widok listy samochodów z wyszukiwarką
    @GetMapping
    public String listaSamochodow(
            @RequestParam(required = false) String marka,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer minRok,
            @RequestParam(required = false) Integer maxRok,
            @RequestParam(required = false) Integer minPrzebieg,
            @RequestParam(required = false) Integer maxPrzebieg,
            @RequestParam(required = false) BigDecimal minCena,
            @RequestParam(required = false) BigDecimal maxCena,
            Model modelAttr) {

        List<Samochod> samochody;

        // Sprawdź czy są parametry wyszukiwania (uwzględniając puste stringi)
        boolean hasSearchParams = (marka != null && !marka.trim().isEmpty()) ||
                (model != null && !model.trim().isEmpty()) ||
                (status != null && !status.trim().isEmpty()) ||
                minRok != null || maxRok != null ||
                minPrzebieg != null || maxPrzebieg != null ||
                minCena != null || maxCena != null;

        if (hasSearchParams) {
            // Użyj zaawansowanego wyszukiwania
            SamochodService.SearchCriteria criteria = new SamochodService.SearchCriteria();
            criteria.setMarka(marka);
            criteria.setModel(model);
            criteria.setStatus(status);
            criteria.setMinRok(minRok);
            criteria.setMaxRok(maxRok);
            criteria.setMinPrzebieg(minPrzebieg);
            criteria.setMaxPrzebieg(maxPrzebieg);
            criteria.setMinCena(minCena);
            criteria.setMaxCena(maxCena);

            samochody = samochodService.searchCars(criteria);

            // DEBUG: Sprawdź wyniki wyszukiwania
            System.out.println("DEBUG: Kryteria wyszukiwania: " + criteria);
            System.out.println("DEBUG: Znaleziono samochodów: " + samochody.size());
        } else {
            // Pokaż wszystkie samochody
            samochody = samochodService.findAll();
        }

        modelAttr.addAttribute("samochody", samochody);
        modelAttr.addAttribute("marki", samochodService.findAllMarki());
        modelAttr.addAttribute("tytul", "Lista Samochodów");
        modelAttr.addAttribute("hasSearchParams", hasSearchParams);

        // Przekaż parametry wyszukiwania z powrotem do formularza
        modelAttr.addAttribute("searchMarka", marka);
        modelAttr.addAttribute("searchModel", model);
        modelAttr.addAttribute("searchStatus", status);
        modelAttr.addAttribute("searchMinRok", minRok);
        modelAttr.addAttribute("searchMaxRok", maxRok);
        modelAttr.addAttribute("searchMinPrzebieg", minPrzebieg);
        modelAttr.addAttribute("searchMaxPrzebieg", maxPrzebieg);
        modelAttr.addAttribute("searchMinCena", minCena);
        modelAttr.addAttribute("searchMaxCena", maxCena);

        return "samochody/lista";
    }

    @GetMapping("/szczegoly")
    public String szczegolySamochodu(@RequestParam("id") String id, Model model, Authentication authentication) {
        Samochod samochod = samochodService.findById(id)
                .orElseThrow(() -> new RuntimeException("Samochód nie znaleziony"));

        model.addAttribute("samochod", samochod);
        model.addAttribute("tytul", samochod.getMarka() + " " + samochod.getModel());

        // DODAJ TĘ CZĘŚĆ: Wyświetlanie ceny z rabatem i saldem
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();

            // Znajdź użytkownika
            userService.findByUsername(username).ifPresent(user -> {
                if (user.getKlient() != null) {
                    Klient klient = user.getKlient();
                    BigDecimal rabat = klient.getProcentPremii();
                    BigDecimal saldo = klient.getSaldoPremii() != null ? klient.getSaldoPremii() : BigDecimal.ZERO;

                    // PRZEKAŻ DANE DO WIDOKU
                    model.addAttribute("klientRabat", rabat);
                    model.addAttribute("klientSaldo", saldo);
                    model.addAttribute("klient", klient);

                    // Oblicz maksymalną kwotę do wykorzystania
                    BigDecimal cenaBazowa = samochod.getCena();
                    BigDecimal maksWykorzystanie = saldo.min(cenaBazowa);
                    model.addAttribute("maksWykorzystanie", maksWykorzystanie);

                    // Oblicz premię, która zostanie naliczona
                    if (rabat != null && rabat.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal premiaOdZakupu = cenaBazowa.multiply(rabat)
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        model.addAttribute("premiaOdZakupu", premiaOdZakupu);
                    }

                    // Oblicz cenę po rabacie
                    if (rabat != null && rabat.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal mnoznikRabatu = BigDecimal.ONE
                                .subtract(rabat.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                        BigDecimal cenaPoRabacie = cenaBazowa.multiply(mnoznikRabatu)
                                .setScale(2, RoundingMode.HALF_UP);

                        model.addAttribute("cenaPoRabacie", cenaPoRabacie);
                    }

                    // Sprawdź czy użytkownik jest tym, który zarezerwował
                    if (samochod.getZarezerwowanyPrzez() != null) {
                        boolean czyZarezerwowanyPrzezeMnie = klient.getId().equals(samochod.getZarezerwowanyPrzez().getId());
                        model.addAttribute("czyZarezerwowanyPrzezeMnie", czyZarezerwowanyPrzezeMnie);
                    } else {
                        model.addAttribute("czyZarezerwowanyPrzezeMnie", false);
                    }
                } else {
                    model.addAttribute("klientRabat", BigDecimal.ZERO);
                    model.addAttribute("klientSaldo", BigDecimal.ZERO);
                    model.addAttribute("czyZarezerwowanyPrzezeMnie", false);
                }
            });
        } else {
            // Dla niezalogowanych
            model.addAttribute("klientRabat", BigDecimal.ZERO);
            model.addAttribute("klientSaldo", BigDecimal.ZERO);
            model.addAttribute("czyZarezerwowanyPrzezeMnie", false);
        }

        return "samochody/szczegoly";
    }

    // Formularz dodawania nowego samochodu - tylko ADMIN
    @GetMapping("/nowy")
    @PreAuthorize("hasRole('ADMIN')")
    public String formNowySamochod(Model model) {
        model.addAttribute("samochod", new Samochod());
        model.addAttribute("tytul", "Dodaj Nowy Samochód");
        return "samochody/form";
    }

    // Zapisywanie nowego samochodu - tylko ADMIN
    @PostMapping("/zapisz")
    @PreAuthorize("hasRole('ADMIN')")
    public String zapiszSamochod(@ModelAttribute Samochod samochod) {
        // Ustaw domyślne wartości dla brakujących pól
        if (samochod.getDataDodania() == null) {
            samochod.setDataDodania(LocalDate.now());
        }
        if (samochod.getRodzajPaliwa() == null) {
            samochod.setRodzajPaliwa("Benzyna");
        }
        if (samochod.getSkrzyniaBiegow() == null) {
            samochod.setSkrzyniaBiegow("Manualna");
        }
        if (samochod.getPojemnoscSilnika() == null) {
            samochod.setPojemnoscSilnika(2.0);
        }

        samochodService.save(samochod);
        return "redirect:/samochody";
    }

    // Formularz edycji samochodu - tylko ADMIN
    @GetMapping("/edytuj")
    @PreAuthorize("hasRole('ADMIN')")
    public String formEdycjaSamochodu(@RequestParam("id") String id, Model model) {
        Samochod samochod = samochodService.findById(id)
                .orElseThrow(() -> new RuntimeException("Samochód nie znaleziony"));
        model.addAttribute("samochod", samochod);
        model.addAttribute("tytul", "Edytuj Samochód");
        return "samochody/form";
    }

    // Aktualizacja samochodu - tylko ADMIN
    @PostMapping("/edytuj")
    @PreAuthorize("hasRole('ADMIN')")
    public String aktualizujSamochod(@RequestParam("id") String id, @ModelAttribute Samochod samochod) {
        samochod.setId(id);
        samochodService.save(samochod);
        return "redirect:/samochody";
    }

    // Usuwanie samochodu - tylko ADMIN
    @PostMapping("/usun")
    @PreAuthorize("hasRole('ADMIN')")
    public String usunSamochod(@RequestParam("id") String id) {
        samochodService.delete(id);
        return "redirect:/samochody";
    }

    // Rezerwacja samochodu - dla zalogowanych użytkowników
    // Poprawiona metoda rezerwacji
    @PostMapping("/zarezerwuj")
    @PreAuthorize("isAuthenticated()")
    public String zarezerwujSamochod(@RequestParam("id") String id,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        try {
            System.out.println("=== ROZPOCZĘCIE REZERWACJI SAMOCHODU ID: " + id + " ===");

            Samochod samochod = samochodService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Samochód nie znaleziony"));

            System.out.println("Samochód: " + samochod.getMarka() + " " + samochod.getModel());
            System.out.println("Status przed rezerwacją: " + samochod.getStatus());

            // Pobierz aktualnego użytkownika
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

            System.out.println("Użytkownik: " + user.getUsername());
            System.out.println("Czy ma klienta: " + (user.getKlient() != null));

            // Upewnij się, że użytkownik ma klienta
            Klient klient = user.getKlient();
            if (klient == null) {
                System.out.println("Brak klienta - tworzę...");
                klient = userService.ensureUserHasKlient(user.getId());
            }

            System.out.println("Klient ID: " + klient.getId());
            System.out.println("Klient: " + klient.getImie() + " " + klient.getNazwisko());

            // Sprawdź czy samochód jest dostępny
            if (!"DOSTEPNY".equals(samochod.getStatus())) {
                String errorMsg = "Samochód nie jest dostępny do rezerwacji. Aktualny status: " + samochod.getStatus();
                System.out.println("BŁĄD: " + errorMsg);
                redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
                return "redirect:/samochody/szczegoly?id=" + id;
            }

            // Zarezerwuj samochód dla tego klienta
            samochod.setStatus("ZAREZERWOWANY");
            samochod.setZarezerwowanyPrzez(klient);
            samochod.setDataRezerwacji(LocalDate.now());

            samochodService.save(samochod);

            System.out.println("Rezerwacja udana! Klient ID: " + klient.getId());
            System.out.println("Samochód zarezerwowany przez: " + samochod.getZarezerwowanyPrzez().getId());
            System.out.println("Status po rezerwacji: " + samochod.getStatus());

            redirectAttributes.addFlashAttribute("successMessage",
                    "Samochód został zarezerwowany pomyślnie! Masz 7 dni na dokonanie zakupu.");

            return "redirect:/samochody/szczegoly?id=" + id;

        } catch (Exception e) {
            System.err.println("BŁĄD podczas rezerwacji: " + e.getMessage());
            e.printStackTrace();

            redirectAttributes.addFlashAttribute("errorMessage",
                    "Błąd podczas rezerwacji: " + e.getMessage());
            return "redirect:/samochody/szczegoly?id=" + id;
        }
    }

    // Zakup samochodu - dla zalogowanych użytkowników
    @PostMapping("/kup")
    @PreAuthorize("isAuthenticated()")
    public String kupSamochod(
            @RequestParam("id") String id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            System.out.println("=== ROZPOCZĘCIE ZAKUPU SAMOCHODU ID: " + id + " ===");

            // 1. Pobierz samochód
            Samochod samochod = samochodService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Samochód nie znaleziony"));

            System.out.println("Samochód: " + samochod.getMarka() + " " + samochod.getModel());
            System.out.println("Status: " + samochod.getStatus());

            // 2. Pobierz użytkownika
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

            System.out.println("Użytkownik: " + user.getUsername());

            // 3. Upewnij się, że użytkownik ma klienta
            Klient klient = user.getKlient();
            if (klient == null) {
                klient = userService.ensureUserHasKlient(user.getId());
                if (klient == null) {
                    throw new RuntimeException("Nie można utworzyć klienta dla użytkownika");
                }
            }

            System.out.println("Klient ID: " + klient.getId());

            // 4. SPRAWDŹ CZY MOŻNA KUPIĆ TEN SAMOCHÓD
            if (!"DOSTEPNY".equals(samochod.getStatus()) && !"ZAREZERWOWANY".equals(samochod.getStatus())) {
                throw new RuntimeException("Samochód nie jest dostępny do sprzedaży. Status: " + samochod.getStatus());
            }

            // 5. Jeśli zarezerwowany, sprawdź czy to TEN SAM klient
            if ("ZAREZERWOWANY".equals(samochod.getStatus())) {
                if (samochod.getZarezerwowanyPrzez() == null) {
                    System.out.println("UWAGA: Samochód zarezerwowany, ale brak przypisanego klienta");
                    // Można kontynuować - być może stara rezerwacja bez przypisania
                } else if (!samochod.getZarezerwowanyPrzez().getId().equals(klient.getId())) {
                    throw new RuntimeException("Ten samochód jest zarezerwowany przez innego klienta. " +
                            "Tylko osoba rezerwująca może go kupić.");
                }
            }

            // 6. Zaktualizuj status samochodu
            samochod.setStatus("SPRZEDANY");
            samochod.setZarezerwowanyPrzez(null); // Wyczyść rezerwację po zakupie
            samochod.setDataRezerwacji(null);
            samochodService.save(samochod);

            System.out.println("Status zmieniony na: SPRZEDANY");

            // 7. Znajdź pracownika (pierwszego z listy)
            Pracownik pracownik = pracownikService.findAll().stream()
                    .findFirst()
                    .orElseGet(() -> {
                        // Jeśli brak pracowników, utwórz domyślnego
                        Pracownik p = new Pracownik();
                        p.setImie("Pracownik");
                        p.setNazwisko("Domyślny");
                        p.setStanowisko("Sprzedawca");
                        p.setEmail("pracownik@komis.pl");
                        p.setTelefon("111-222-333");
                        p.setDataZatrudnienia(LocalDate.now());
                        return pracownikService.save(p);
                    });

            System.out.println("Pracownik: " + pracownik.getImie() + " " + pracownik.getNazwisko());

            // 8. Oblicz cenę z rabatem
            BigDecimal cenaBazowa = samochod.getCena();
            BigDecimal rabatProcent = klient.getProcentPremii() != null ?
                    klient.getProcentPremii() : BigDecimal.ZERO;

            BigDecimal cenaZRabatem = cenaBazowa;
            if (rabatProcent.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal mnoznikRabatu = BigDecimal.ONE
                        .subtract(rabatProcent.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                cenaZRabatem = cenaBazowa.multiply(mnoznikRabatu)
                        .setScale(2, RoundingMode.HALF_UP);

                System.out.println("Zastosowano rabat: " + rabatProcent + "%");
                System.out.println("Cena po rabacie: " + cenaZRabatem);
            }

            // 9. Oblicz premię, która zostanie dodana do salda
            BigDecimal naliczonaPremia = BigDecimal.ZERO;
            if (rabatProcent.compareTo(BigDecimal.ZERO) > 0) {
                naliczonaPremia = cenaBazowa.multiply(rabatProcent)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                System.out.println("Naliczona premia: " + naliczonaPremia + " zł");
            }

            // 10. Utwórz zakup
            Zakup zakup = Zakup.builder()
                    .samochod(samochod)
                    .klient(klient)
                    .pracownik(pracownik)
                    .dataZakupu(LocalDate.now())
                    .cenaBazowa(cenaBazowa)
                    .cenaZakupu(cenaZRabatem)
                    .zastosowanyRabat(rabatProcent)
                    .naliczonaPremia(naliczonaPremia)
                    .wykorzystaneSaldo(BigDecimal.ZERO) // Normalny zakup bez wykorzystania salda
                    .build();

            Zakup zapisanyZakup = zakupService.save(zakup);
            System.out.println("Zakup zapisany! ID: " + zapisanyZakup.getId());

            // 11. Aktualizuj dane klienta (liczba zakupów, wydane kwoty)
            klient.setLiczbaZakupow(klient.getLiczbaZakupow() + 1);
            klient.setTotalWydane(klient.getTotalWydane().add(cenaZRabatem));

            // Aktualizuj procent premii na podstawie liczby zakupów
            if (klient.getLiczbaZakupow() >= 5) {
                klient.setProcentPremii(new BigDecimal("15"));
            } else if (klient.getLiczbaZakupow() >= 3) {
                klient.setProcentPremii(new BigDecimal("10"));
            } else if (klient.getLiczbaZakupow() >= 2) {
                klient.setProcentPremii(new BigDecimal("5"));
            } else {
                klient.setProcentPremii(BigDecimal.ZERO);
            }

            // Dodaj premię do salda klienta
            klient.setSaldoPremii(klient.getSaldoPremii().add(naliczonaPremia));

            klientService.save(klient);
            System.out.println("Zaktualizowano dane klienta");
            System.out.println("Nowa liczba zakupów: " + klient.getLiczbaZakupow());
            System.out.println("Nowe saldo: " + klient.getSaldoPremii() + " zł");
            System.out.println("Nowy procent premii: " + klient.getProcentPremii() + "%");

            // 12. Przygotuj komunikat
            String message;
            if (rabatProcent.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal zaoszczedzone = cenaBazowa.subtract(cenaZRabatem);
                message = String.format(
                        "Samochód kupiony!<br>" +
                                "Zastosowano rabat: <strong>%.0f%%</strong><br>" +
                                "Zaoszczędziłeś: <strong>%.2f zł</strong><br>" +
                                "Na Twoje saldo dodano: <strong>%.2f zł</strong> premii",
                        rabatProcent, zaoszczedzone, naliczonaPremia
                );
            } else {
                message = "Samochód kupiony pomyślnie! Zakup zarejestrowany w systemie.";
            }

            redirectAttributes.addFlashAttribute("successMessage", message);
            System.out.println("=== ZAKUP ZAKOŃCZONY SUKCESEM ===");

        } catch (Exception e) {
            System.err.println("BŁĄD podczas zakupu: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Błąd podczas zakupu: " + e.getMessage());
        }

        return "redirect:/samochody/szczegoly?id=" + id;
    }

    // Poprawiona metoda anulowania rezerwacji
    @PostMapping("/anuluj-rezerwacje")
    @PreAuthorize("isAuthenticated()")
    public String anulujRezerwacje(@RequestParam("id") String id,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            System.out.println("=== ANULOWANIE REZERWACJI SAMOCHODU ID: " + id + " ===");

            Samochod samochod = samochodService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Samochód nie znaleziony"));

            System.out.println("Samochód: " + samochod.getMarka() + " " + samochod.getModel());
            System.out.println("Status: " + samochod.getStatus());
            System.out.println("Zarezerwowany przez: " +
                    (samochod.getZarezerwowanyPrzez() != null ?
                            samochod.getZarezerwowanyPrzez().getId() : "null"));

            // Pobierz aktualnego użytkownika
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

            Klient klient = user.getKlient();
            if (klient == null) {
                throw new RuntimeException("Nie masz powiązanego klienta");
            }

            System.out.println("Klient ID: " + klient.getId());

            // Sprawdź czy samochód jest zarezerwowany i czy to ten klient go zarezerwował
            if (!"ZAREZERWOWANY".equals(samochod.getStatus())) {
                String errorMsg = "Samochód nie jest zarezerwowany. Aktualny status: " + samochod.getStatus();
                System.out.println("BŁĄD: " + errorMsg);
                redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
                return "redirect:/samochody/szczegoly?id=" + id;
            }

            if (samochod.getZarezerwowanyPrzez() == null ||
                    !samochod.getZarezerwowanyPrzez().getId().equals(klient.getId())) {
                String errorMsg = "Ten samochód został zarezerwowany przez innego klienta. " +
                        "Tylko osoba rezerwująca może anulować rezerwację.";
                System.out.println("BŁĄD: " + errorMsg);
                System.out.println("Zarezerwowany przez: " +
                        (samochod.getZarezerwowanyPrzez() != null ?
                                samochod.getZarezerwowanyPrzez().getId() : "null"));
                System.out.println("Aktualny klient: " + klient.getId());

                redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
                return "redirect:/samochody/szczegoly?id=" + id;
            }

            // Anuluj rezerwację
            samochod.setStatus("DOSTEPNY");
            samochod.setZarezerwowanyPrzez(null);
            samochod.setDataRezerwacji(null);

            samochodService.save(samochod);

            System.out.println("Rezerwacja anulowana pomyślnie!");

            redirectAttributes.addFlashAttribute("successMessage",
                    "Rezerwacja została anulowana. Samochód jest znów dostępny.");

            return "redirect:/samochody/szczegoly?id=" + id;

        } catch (Exception e) {
            System.err.println("BŁĄD podczas anulowania rezerwacji: " + e.getMessage());
            e.printStackTrace();

            redirectAttributes.addFlashAttribute("errorMessage",
                    "Błąd podczas anulowania rezerwacji: " + e.getMessage());
            return "redirect:/samochody/szczegoly?id=" + id;
        }
    }

    // Pomocnicze metody do ekstrakcji imienia i nazwiska z username
    private String extractFirstName(String username) {
        if (username == null || username.isEmpty()) return "Użytkownik";
        String[] parts = username.split("\\.");
        if (parts.length > 0) return capitalize(parts[0]);
        return capitalize(username);
    }

    private String extractLastName(String username) {
        if (username == null || username.isEmpty()) return "Klient";
        String[] parts = username.split("\\.");
        if (parts.length > 1) return capitalize(parts[1]);
        return "Klient";
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    /**
     * Oblicza premię, która zostanie naliczona na saldo klienta
     */
    private BigDecimal obliczPremie(BigDecimal cena, BigDecimal procentPremii) {
        if (procentPremii == null || procentPremii.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return cena.multiply(procentPremii)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /**
     * Oblicza maksymalną kwotę, jaką klient może wykorzystać z salda
     */
    private BigDecimal obliczMaksWykorzystanie(BigDecimal saldoKlienta, BigDecimal cenaSamochodu) {
        if (saldoKlienta == null || saldoKlienta.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        // Można wykorzystać maksymalnie tyle, ile wynosi saldo lub cena samochodu
        return saldoKlienta.min(cenaSamochodu);
    }
}