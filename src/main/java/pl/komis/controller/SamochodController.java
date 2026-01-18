package pl.komis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.komis.model.*;
import pl.komis.service.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Controller
@RequestMapping("/samochody")
@RequiredArgsConstructor
public class SamochodController {

    private final SamochodService samochodService;
    private final UserService userService;
    private final KlientService klientService;
    private final ZakupService zakupService;
    private final PracownikService pracownikService;

    private static final String UPLOAD_DIR = "uploads/";

    // Pomocnicza metoda do pobierania Samochodu (obsługuje Optional)
    private Samochod getSamochodById(String id) {
        // Sprawdź czy serwis zwraca Optional czy bezpośredni obiekt
        Object result = samochodService.findById(id);

        if (result instanceof Optional) {
            return ((Optional<Samochod>) result)
                    .orElseThrow(() -> new RuntimeException("Samochód nie znaleziony"));
        } else {
            Samochod samochod = (Samochod) result;
            if (samochod == null) {
                throw new RuntimeException("Samochód nie znaleziony");
            }
            return samochod;
        }
    }

    // Pomocnicza metoda do pobierania User (obsługuje Optional)
    private User getUserByUsername(String username) {
        Object result = userService.findByUsername(username);

        if (result instanceof Optional) {
            return ((Optional<User>) result)
                    .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));
        } else {
            User user = (User) result;
            if (user == null) {
                throw new RuntimeException("Użytkownik nie znaleziony");
            }
            return user;
        }
    }

    // Pomocnicza metoda do pobierania Klient (obsługuje Optional)
    private Klient getKlientById(String id) {
        Object result = klientService.findById(id);

        if (result instanceof Optional) {
            return ((Optional<Klient>) result)
                    .orElseThrow(() -> new RuntimeException("Klient nie znaleziony"));
        } else {
            Klient klient = (Klient) result;
            if (klient == null) {
                throw new RuntimeException("Klient nie znaleziony");
            }
            return klient;
        }
    }

    @GetMapping("/szczegoly")
    public String szczegolySamochodu(@RequestParam("id") String id, Model model, Authentication authentication) {
        try {
            Samochod samochod = getSamochodById(id);
            model.addAttribute("samochod", samochod);
            model.addAttribute("tytul", samochod.getMarka() + " " + samochod.getModel());

            // Inicjalizuj zmienne dla niezalogowanych użytkowników
            Double klientRabat = 0.0;
            Double klientSaldo = 0.0;
            Double cenaPoRabacie = samochod.getCena(); // ZMIANA: pełna cena
            Double premiaOdZakupu = 0.0;
            Double maksWykorzystanie = 0.0;
            boolean czyZarezerwowanyPrzezeMnie = false;

            // Jeśli użytkownik jest zalogowany
            if (authentication != null && authentication.isAuthenticated()) {
                String username = authentication.getName();
                User user = getUserByUsername(username);

                // Sprawdź czy użytkownik ma powiązanego klienta
                if (user.getKlientId() != null) {
                    Klient klient = getKlientById(user.getKlientId());

                    // Pobierz rabat klienta
                    klientRabat = klient.getProcentPremii() != null ?
                            klient.getProcentPremii().doubleValue() : 0.0;
                    klientSaldo = klient.getSaldoPremii() != null ?
                            klient.getSaldoPremii().doubleValue() : 0.0;

                    // NIE obniżamy ceny! Pokazujemy pełną cenę.
                    cenaPoRabacie = samochod.getCena(); // ZMIANA: zawsze pełna cena

                    // Oblicz premię od zakupu (ale nie odejmujemy od ceny!)
                    if (klientRabat > 0.0) {
                        premiaOdZakupu = samochod.getCena() * (klientRabat / 100.0);
                        premiaOdZakupu = Math.round(premiaOdZakupu * 100.0) / 100.0;
                    }

                    // Oblicz maksymalne wykorzystanie salda (20% ceny LUB saldo, co jest mniejsze)
                    Double maksProcentowo = samochod.getCena() * 0.20; // 20% pełnej ceny
                    maksWykorzystanie = Math.min(maksProcentowo, klientSaldo);
                    maksWykorzystanie = Math.round(maksWykorzystanie * 100.0) / 100.0;

                    // Sprawdź czy użytkownik zarezerwował ten samochód
                    if (samochod.getZarezerwowanyPrzezKlientId() != null) {
                        czyZarezerwowanyPrzezeMnie = samochod.getZarezerwowanyPrzezKlientId()
                                .equals(klient.getId());
                    }
                }
            }

            // Dodaj atrybuty do modelu
            model.addAttribute("klientRabat", klientRabat);
            model.addAttribute("klientSaldo", klientSaldo);
            model.addAttribute("cenaPoRabacie", cenaPoRabacie); // To teraz pełna cena
            model.addAttribute("premiaOdZakupu", premiaOdZakupu);
            model.addAttribute("maksWykorzystanie", maksWykorzystanie);
            model.addAttribute("czyZarezerwowanyPrzezeMnie", czyZarezerwowanyPrzezeMnie);

            return "samochody/szczegoly";

        } catch (RuntimeException e) {
            model.addAttribute("error", "Samochód nie został znaleziony");
            return "error";
        }
    }

    @GetMapping("/edytuj")
    @PreAuthorize("hasRole('ADMIN')")
    public String formEdycjaSamochodu(@RequestParam("id") String id, Model model) {
        Samochod samochod = getSamochodById(id);
        model.addAttribute("samochod", samochod);
        model.addAttribute("tytul", "Edytuj Samochód");
        return "samochody/form";
    }


    private void bezpiecznieUsunPlikZdjęcia(String nazwaPliku, String aktualneSamochodId) {
        if (nazwaPliku == null || nazwaPliku.isEmpty()) {
            return;
        }

        try {
            boolean jestUzywanePrzezInne = false;
            List<Samochod> wszystkieSamochody = samochodService.findAll();

            for (Samochod samochod : wszystkieSamochody) {
                if (samochod.getZdjecieNazwa() != null &&
                        samochod.getZdjecieNazwa().equals(nazwaPliku) &&
                        !samochod.getId().equals(aktualneSamochodId)) {
                    jestUzywanePrzezInne = true;
                    break;
                }
            }

            if (!jestUzywanePrzezInne) {
                Path filePath = Paths.get(UPLOAD_DIR + nazwaPliku);
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            System.err.println("Błąd usuwania pliku: " + e.getMessage());
        }
    }

    @PostMapping("/usun")
    @PreAuthorize("hasRole('ADMIN')")
    public String usunSamochod(@RequestParam("id") String id, RedirectAttributes redirectAttributes) {
        try {
            Samochod samochod = getSamochodById(id);

            String zdjecieNazwa = samochod.getZdjecieNazwa();
            boolean isUsedElsewhere = false;

            if (zdjecieNazwa != null && !zdjecieNazwa.isEmpty()) {
                List<Samochod> allCars = samochodService.findAll();
                for (Samochod car : allCars) {
                    if (!car.getId().equals(id) && zdjecieNazwa.equals(car.getZdjecieNazwa())) {
                        isUsedElsewhere = true;
                        break;
                    }
                }

                if (!isUsedElsewhere) {
                    Path filePath = Paths.get(UPLOAD_DIR + zdjecieNazwa);
                    Files.deleteIfExists(filePath);
                }
            }

            samochodService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Samochód usunięty pomyślnie");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Błąd: " + e.getMessage());
        }
        return "redirect:/samochody";
    }
    @GetMapping("/nowy")
    @PreAuthorize("hasRole('ADMIN')")
    public String formNowySamochod(Model model) {
        model.addAttribute("samochod", new Samochod());
        model.addAttribute("tytul", "Dodaj nowy samochód");
        return "samochody/form";
    }

    @PostMapping("/zapisz")
    @PreAuthorize("hasRole('ADMIN')")
    public String zapiszSamochod(
            @ModelAttribute Samochod samochod,
            @RequestParam(value = "zdjeciePlik", required = false) MultipartFile zdjeciePlik,
            @RequestParam(value = "usunZdjecie", required = false, defaultValue = "false") boolean usunZdjecie,
            RedirectAttributes redirectAttributes) {

        try {
            // FIX: Zamień pusty string na null dla nowego samochodu
            if (samochod.getId() != null && samochod.getId().trim().isEmpty()) {
                samochod.setId(null);
            }

            // Jeśli to nowy samochód (brak ID)
            if (samochod.getId() == null) {
                samochod.setDataDodania(LocalDate.now());
                samochod.setStatus("DOSTEPNY");

                // Obsługa zdjęcia dla nowego samochodu
                if (zdjeciePlik != null && !zdjeciePlik.isEmpty()) {
                    // Zapisz nowe zdjęcie
                    Path uploadPath = Paths.get(UPLOAD_DIR);
                    if (!Files.exists(uploadPath)) {
                        Files.createDirectories(uploadPath);
                    }

                    String originalFileName = zdjeciePlik.getOriginalFilename();
                    String fileExtension = "";
                    if (originalFileName != null && originalFileName.contains(".")) {
                        fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
                    }
                    String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

                    Path filePath = uploadPath.resolve(uniqueFileName);
                    Files.copy(zdjeciePlik.getInputStream(), filePath);
                    samochod.setZdjecieNazwa(uniqueFileName);
                } else {
                    samochod.setZdjecieNazwa("domyslny.jpg");
                }
            } else {
                // Jeśli to edycja istniejącego samochodu
                Samochod starySamochod = getSamochodById(samochod.getId());
                samochod.setDataDodania(starySamochod.getDataDodania());

                // Obsługa zdjęcia (logika z metody edytuj)
                if (usunZdjecie) {
                    samochod.setZdjecieNazwa(null);
                    if (starySamochod.getZdjecieNazwa() != null) {
                        bezpiecznieUsunPlikZdjęcia(starySamochod.getZdjecieNazwa(), samochod.getId());
                    }
                } else if (zdjeciePlik != null && !zdjeciePlik.isEmpty()) {
                    // Zapisz nowe zdjęcie
                    Path uploadPath = Paths.get(UPLOAD_DIR);
                    if (!Files.exists(uploadPath)) {
                        Files.createDirectories(uploadPath);
                    }

                    String originalFileName = zdjeciePlik.getOriginalFilename();
                    String fileExtension = "";
                    if (originalFileName != null && originalFileName.contains(".")) {
                        fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
                    }
                    String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

                    Path filePath = uploadPath.resolve(uniqueFileName);
                    Files.copy(zdjeciePlik.getInputStream(), filePath);

                    if (starySamochod.getZdjecieNazwa() != null) {
                        bezpiecznieUsunPlikZdjęcia(starySamochod.getZdjecieNazwa(), samochod.getId());
                    }

                    samochod.setZdjecieNazwa(uniqueFileName);
                } else {
                    samochod.setZdjecieNazwa(starySamochod.getZdjecieNazwa());
                }
            }

            // Ustaw domyślne wartości
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
            redirectAttributes.addFlashAttribute("successMessage",
                    samochod.getId() == null ? "Samochód dodany pomyślnie" : "Samochód zaktualizowany pomyślnie");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Błąd: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/samochody" + (samochod.getId() != null ? "/edytuj?id=" + samochod.getId() : "/nowy");
        }

        return "redirect:/samochody";
    }

    @PostMapping("/zarezerwuj")
    @PreAuthorize("isAuthenticated()")
    public String zarezerwujSamochod(@RequestParam("id") String id,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        try {
            System.out.println("=== ROZPOCZĘCIE REZERWACJI SAMOCHODU ID: " + id + " ===");

            Samochod samochod = getSamochodById(id);
            System.out.println("Samochód: " + samochod.getMarka() + " " + samochod.getModel());
            System.out.println("Status przed rezerwacją: " + samochod.getStatus());

            String username = authentication.getName();
            User user = getUserByUsername(username);

            System.out.println("Użytkownik: " + user.getUsername());
            System.out.println("Czy ma klienta: " + (user.getKlientId() != null));

            String klientId = user.getKlientId();
            if (klientId == null) {
                System.out.println("Brak klienta - tworzę...");
                Klient klient = userService.ensureUserHasKlient(user.getId());
                klientId = klient.getId();
            }

            Klient klient = getKlientById(klientId);
            System.out.println("Klient ID: " + klient.getId());
            System.out.println("Klient: " + klient.getImie() + " " + klient.getNazwisko());

            if (!"DOSTEPNY".equals(samochod.getStatus())) {
                String errorMsg = "Samochód nie jest dostępny do rezerwacji. Aktualny status: " + samochod.getStatus();
                System.out.println("BŁĄD: " + errorMsg);
                redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
                return "redirect:/samochody/szczegoly?id=" + id;
            }

            samochod.setStatus("ZAREZERWOWANY");
            samochod.setZarezerwowanyPrzezKlientId(klientId);
            samochod.setDataRezerwacji(LocalDate.now());

            samochodService.save(samochod);

            System.out.println("Rezerwacja udana! Klient ID: " + klientId);
            System.out.println("Samochód zarezerwowany przez klienta ID: " + samochod.getZarezerwowanyPrzezKlientId());
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

    @PostMapping("/kup")
    @PreAuthorize("isAuthenticated()")
    public String kupSamochod(
            @RequestParam("id") String id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            System.out.println("=== ROZPOCZĘCIE ZAKUPU SAMOCHODU ID: " + id + " ===");

            Samochod samochod = getSamochodById(id);
            System.out.println("Samochód: " + samochod.getMarka() + " " + samochod.getModel());
            System.out.println("Status: " + samochod.getStatus());

            String username = authentication.getName();
            User user = getUserByUsername(username);
            System.out.println("Użytkownik: " + user.getUsername());

            String klientId = user.getKlientId();
            if (klientId == null) {
                Klient klient = userService.ensureUserHasKlient(user.getId());
                klientId = klient.getId();
                if (klientId == null) {
                    throw new RuntimeException("Nie można utworzyć klienta dla użytkownika");
                }
            }

            Klient klient = getKlientById(klientId);
            System.out.println("Klient ID: " + klient.getId());

            if (!"DOSTEPNY".equals(samochod.getStatus()) && !"ZAREZERWOWANY".equals(samochod.getStatus())) {
                throw new RuntimeException("Samochód nie jest dostępny do sprzedaży. Status: " + samochod.getStatus());
            }

            if ("ZAREZERWOWANY".equals(samochod.getStatus())) {
                if (samochod.getZarezerwowanyPrzezKlientId() == null) {
                    System.out.println("UWAGA: Samochód zarezerwowany, ale brak przypisanego klienta");
                } else if (!samochod.getZarezerwowanyPrzezKlientId().equals(klientId)) {
                    throw new RuntimeException("Ten samochód jest zarezerwowany przez innego klienta. " +
                            "Tylko osoba rezerwująca może go kupić.");
                }
            }

            samochod.setStatus("SPRZEDANY");
            samochod.setZarezerwowanyPrzezKlientId(null);
            samochod.setDataRezerwacji(null);
            samochodService.save(samochod);

            System.out.println("Status zmieniony na: SPRZEDANY");

            Pracownik pracownik = pracownikService.findAll().stream()
                    .findFirst()
                    .orElseGet(() -> {
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

            Double cenaBazowa = samochod.getCena();
            Double rabatProcent = klient.getProcentPremii() != null ?
                    klient.getProcentPremii().doubleValue() : 0.0;

            // POPRAWIONE: Nie obniżamy ceny o premię!
            // Cena zakupu = cena bazowa (nie odejmujemy premii!)
            Double cenaZakupu = cenaBazowa; // ZMIANA: nie obniżamy ceny o premię!
            System.out.println("Cena zakupu: " + cenaZakupu + " zł (pełna cena)");

            Double naliczonaPremia = 0.0;
            if (rabatProcent > 0.0) {
                naliczonaPremia = cenaBazowa * (rabatProcent / 100.0);
                naliczonaPremia = Math.round(naliczonaPremia * 100.0) / 100.0;
                System.out.println("Naliczona premia: " + naliczonaPremia + " zł");
            }

            Zakup zakup = Zakup.builder()
                    .samochodId(samochod.getId())
                    .klientId(klient.getId())
                    .pracownikId(pracownik.getId())
                    .samochodMarka(samochod.getMarka())
                    .samochodModel(samochod.getModel())
                    .klientImieNazwisko(klient.getImie() + " " + klient.getNazwisko())
                    .pracownikImieNazwisko(pracownik.getImie() + " " + pracownik.getNazwisko())
                    .dataZakupu(LocalDate.now())
                    .cenaBazowa(cenaBazowa)
                    .cenaZakupu(cenaZakupu) // ZMIANA: pełna cena, nie obniżona
                    .zastosowanyRabat(rabatProcent)
                    .naliczonaPremia(naliczonaPremia)
                    .wykorzystaneSaldo(0.0)
                    .build();

            Zakup zapisanyZakup = zakupService.save(zakup);
            System.out.println("Zakup zapisany! ID: " + zapisanyZakup.getId());

            klient.setLiczbaZakupow(klient.getLiczbaZakupow() + 1);
            klient.setTotalWydane(klient.getTotalWydane() + cenaZakupu);

            if (klient.getLiczbaZakupow() >= 5) {
                klient.setProcentPremii(15.0);
            } else if (klient.getLiczbaZakupow() >= 3) {
                klient.setProcentPremii(10.0);
            } else if (klient.getLiczbaZakupow() >= 2) {
                klient.setProcentPremii(5.0);
            } else {
                klient.setProcentPremii(0.0);
            }

            // Dodaj premię do salda klienta
            klient.setSaldoPremii(klient.getSaldoPremii() + naliczonaPremia);

            klientService.save(klient);
            System.out.println("Zaktualizowano dane klienta");
            System.out.println("Nowa liczba zakupów: " + klient.getLiczbaZakupow());
            System.out.println("Nowe saldo: " + klient.getSaldoPremii() + " zł");
            System.out.println("Nowy procent premii: " + klient.getProcentPremii() + "%");

            String message;
            if (rabatProcent > 0.0) {
                // ZMIANA: Nie pokazujemy "zaoszczędzonej" kwoty, bo nie obniżamy ceny
                message = String.format(
                        "Samochód kupiony!<br>" +
                                "Naliczono premię: <strong>%.2f zł</strong><br>" +
                                "Twoje saldo: <strong>%.2f zł</strong>",
                        naliczonaPremia, klient.getSaldoPremii()
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

    @PostMapping("/anuluj-rezerwacje")
    @PreAuthorize("isAuthenticated()")
    public String anulujRezerwacje(@RequestParam("id") String id,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            Samochod samochod = getSamochodById(id);
            String username = authentication.getName();
            User user = getUserByUsername(username);
            String klientId = user.getKlientId();

            if (klientId == null) {
                throw new RuntimeException("Nie masz powiązanego klienta");
            }

            Klient klient = getKlientById(klientId);

            // Sprawdź status
            String status = samochod.getStatus() != null ? samochod.getStatus().trim() : "";
            if (!status.equalsIgnoreCase("ZAREZERWOWANY")) {
                String errorMsg = "Samochód nie jest zarezerwowany. Aktualny status: " + samochod.getStatus();
                redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
                return "redirect:/samochody/szczegoly?id=" + id;
            }

            // Sprawdź czy użytkownik jest właścicielem rezerwacji
            boolean czyRowne = false;
            if (samochod.getZarezerwowanyPrzezKlientId() != null && klient.getId() != null) {
                czyRowne = samochod.getZarezerwowanyPrzezKlientId().trim()
                        .equals(klient.getId().trim());
            }

            if (samochod.getZarezerwowanyPrzezKlientId() == null || !czyRowne) {
                String errorMsg = "Ten samochód został zarezerwowany przez innego klienta. " +
                        "Tylko osoba rezerwująca może anulować rezerwację.";
                redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
                return "redirect:/samochody/szczegoly?id=" + id;
            }

            // Anuluj rezerwację
            samochod.setStatus("DOSTEPNY");
            samochod.setZarezerwowanyPrzezKlientId(null);
            samochod.setDataRezerwacji(null);
            samochodService.save(samochod);

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

    @GetMapping
    public String listSamochody(
            @RequestParam(value = "marka", required = false) String marka,
            @RequestParam(value = "model", required = false) String model,
            @RequestParam(value = "status", required = false) String status,
            Model modelAttribute) {

        // Pobierz listę samochodów (z filtrami jeśli są)
        List<Samochod> samochody;

        if ((marka != null && !marka.isEmpty()) ||
                (model != null && !model.isEmpty()) ||
                (status != null && !status.isEmpty())) {
            // Użyj wyszukiwania z filtrami
            samochody = samochodService.searchCarsSimple(marka, model, status);
        } else {
            // Pobierz wszystkie samochody
            samochody = samochodService.findAll();
        }

        // ZAWSZE upewnij się, że lista nie jest null
        if (samochody == null) {
            samochody = new ArrayList<>();
        }

        // Oblicz statystyki
        long dostepneCount = samochody.stream()
                .filter(s -> "DOSTEPNY".equals(s.getStatus()))
                .count();

        long zarezerwowaneCount = samochody.stream()
                .filter(s -> "ZAREZERWOWANY".equals(s.getStatus()))
                .count();

        long sprzedaneCount = samochody.stream()
                .filter(s -> "SPRZEDANY".equals(s.getStatus()))
                .count();

        // Pobierz unikalne marki dla filtrowania
        List<String> marki = samochodService.findAllMarki();

        // Dodaj atrybuty do modelu
        modelAttribute.addAttribute("samochody", samochody);
        modelAttribute.addAttribute("marki", marki);
        modelAttribute.addAttribute("dostepneCount", dostepneCount);
        modelAttribute.addAttribute("zarezerwowaneCount", zarezerwowaneCount);
        modelAttribute.addAttribute("sprzedaneCount", sprzedaneCount);
        modelAttribute.addAttribute("searchMarka", marka);
        modelAttribute.addAttribute("searchModel", model);
        modelAttribute.addAttribute("searchStatus", status);
        modelAttribute.addAttribute("hasSearchParams",
                (marka != null && !marka.isEmpty()) ||
                        (model != null && !model.isEmpty()) ||
                        (status != null && !status.isEmpty()));

        return "samochody/lista";
    }
}